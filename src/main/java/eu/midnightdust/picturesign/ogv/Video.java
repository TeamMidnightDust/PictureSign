package eu.midnightdust.picturesign.ogv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.lwjgl.system.MemoryUtil;

import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;

import eu.midnightdust.picturesign.ogv.Ogv.VideoFrame;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.openal.AL11.*;

public class Video {

	private static final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(30))
			.build();
	
	private IntBuffer nativeBuffer;
	
	private static final AtomicInteger nextId = new AtomicInteger();
	
	private volatile AtomicBoolean alive = new AtomicBoolean(true);
	private volatile boolean reset = false;
	
	private String url = null;
	private boolean playing = false;
	private volatile long time = 0;
	
	private volatile ArrayBlockingQueue<VideoFrame> videoQueue = new ArrayBlockingQueue<>(30);
	
	private final Identifier textureId;
	private final AbstractTexture tex;
	private volatile Ogv ogv;
	private boolean repeat = false;
	private Thread thread;
	private volatile int alSourceName = -1;
	private final ArrayDeque<Integer> unusedBuffers = new ArrayDeque<>();
	private final IntList allBuffers = new IntArrayList();

	private volatile long seekTarget = -1;
	private volatile boolean audioReady = false;
	
	private long nextFrameUpdate = 0;
	
	public Video(Identifier id) {
		this.textureId = new Identifier("dynamic/picturesign/video_"+nextId.getAndIncrement());
		tex = new AbstractTexture() {
			@Override public void load(ResourceManager manager) throws IOException {}
		};
		tex.setFilter(true, false);
		MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, tex);
	}
	
	public void update() {
		if (ogv != null && playing) {
			var now = System.nanoTime();
			
			if (!audioReady && videoQueue.remainingCapacity() > 0) {
				// don't play video until audio is ready unless the video queue is full
				return;
			}
			
			VideoFrame frame = null;
			while (now > nextFrameUpdate) {
				frame = videoQueue.poll();
				if (frame == null) return;
				if (nextFrameUpdate == 0) {
					nextFrameUpdate = now;
				} else {
					nextFrameUpdate += frame.durationNs();
				}
			}
			
			if (frame == null) return;
			if (nativeBuffer == null) {
				nativeBuffer = MemoryUtil.memCallocInt(frame.rgba().length);
			} else if (nativeBuffer.capacity() < frame.rgba().length) {
				nativeBuffer = MemoryUtil.memRealloc(nativeBuffer, frame.rgba().length);
			}
			nativeBuffer.clear();
			nativeBuffer.put(frame.rgba()).flip();
			tex.bindTexture();
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, frame.width(), frame.height(), 0, GL_BGRA, GL_UNSIGNED_BYTE, nativeBuffer);
			
			if (alSourceName != -1) {
				alSourcef(alSourceName, AL_GAIN, MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER));
				if (alGetSourcei(alSourceName, AL_SOURCE_STATE) != AL_PLAYING) {
					alSourcePlay(alSourceName);
				}
			}
		}
	}
	
	public void destroy() {
		if (ogv != null) {
			try {
				ogv.close();
			} catch (IOException e) {}
		}
		playing = false;
		alive.set(false);
		if (alSourceName != -1) {
			alDeleteSources(alSourceName);
			alDeleteBuffers(allBuffers.toIntArray());
			allBuffers.clear();
			alSourceName = -1;
		}
		MinecraftClient.getInstance().getTextureManager().destroyTexture(textureId);
	}
	
	public void stop() {
		playing = false;
	}

	public void play(String url) {
		if (thread != null) {
			if (url.equals(this.url)) {
				playing = true;
				return;
			}
			if (alSourceName != -1) {
				alDeleteSources(alSourceName);
				alDeleteBuffers(allBuffers.toIntArray());
				allBuffers.clear();
				alSourceName = -1;
			}
			var oldVideoQueue = this.videoQueue;
			var oldAlive = this.alive;
			videoQueue = new ArrayBlockingQueue<>(10);
			alive = new AtomicBoolean(true);
			// clear old queues to unblock the thread so it realizes it should exit
			oldAlive.set(false);
			oldVideoQueue.clear();
		}
		playing = true;
		this.url = url;
		// make local references so we can abandon old threads without having to wait for them to exit
		var alive = this.alive;
		var videoQueue = this.videoQueue;
		thread = new Thread(() -> {
			Path tmpFile = null;
			try {
				var req = HttpRequest.newBuilder(URI.create(url))
						.header("User-Agent", "PictureSign Theora")
						.header("Accept", "video/ogg; codecs=\"theora, vorbis\"")
						.build();
				ByteSource src;
				if (repeat) {
					// assume it's a short video that we don't want to be constantly redownloading
					tmpFile = Files.createTempFile("picturesign", ".ogv");
					src = retrying(req, BodyHandlers.ofFile(tmpFile), MoreFiles::asByteSource);
				} else {
					src = new ByteSource() {
						@Override
						public InputStream openStream() throws IOException {
							try {
								return retrying(req, BodyHandlers.ofInputStream(), t -> t);
							} catch (InterruptedException e) {
								throw new InterruptedIOException();
							}
						}
					};
				}
				if (src == null) return;
				ogv = new Ogv(src.openStream());
				if (ogv.getVorbisInfo().channels == 0) {
					// there will be no audio data, let the video run free
					audioReady = true;
				}
				// I'm not totally sure why this is necessary
				int audioFrameWarmup = 9;
				while (alive.get()) {
					ogv.step();
					if (ogv.isEnd() || reset) {
						ogv.close();
						if (repeat || reset) {
							reset = false;
							ogv = new Ogv(src.openStream());
							audioFrameWarmup = 9;
							continue;
						} else {
							ogv = null;
						}
						playing = false;
						break;
					}
					var vid = ogv.getVideo();
					if (vid != null) {
						var info = ogv.getTheoraInfo();
						time += (info.fps_denominator*1000)/info.fps_numerator;
					}
					var aud = ogv.getAudio();
					if (seekTarget != -1) {
						if (time < seekTarget) continue;
						seekTarget = -1;
					}
					if (aud != null) {
						if (alSourceName == -1) {
							alSourceName = alGenSources();
							alSourcef(alSourceName, AL_GAIN, MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER));
						}
						
						if (audioFrameWarmup > 0) {
							audioFrameWarmup--;
						} else {
							int[] unqueuedNames = new int[1];
							while (true) {
								unqueuedNames[0] = 0;
								alSourceUnqueueBuffers(alSourceName, unqueuedNames);
								if (unqueuedNames[0] != 0) {
									unusedBuffers.add(unqueuedNames[0]);
								} else {
									break;
								}
							}
							
							var buffer = unusedBuffers.poll();
							if (buffer == null) {
								buffer = alGenBuffers();
								allBuffers.add(buffer.intValue());
							}
							alBufferData(buffer, ogv.getVorbisInfo().channels == 2 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16, aud, ogv.getVorbisInfo().rate);
							alSourceQueueBuffers(alSourceName, buffer);
							audioReady = true;
						}
					}
					if (vid != null) {
						videoQueue.put(vid);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("IO exception while decoding video from "+url);
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			} catch (IllegalArgumentException e) {
				// bad url
			} finally {
				try {
					if (tmpFile != null) Files.delete(tmpFile);
				} catch (IOException e) {}
			}
		}, "Video decode thread ("+url+")");
		thread.setDaemon(true);
		thread.start();
	}
	
	private static <T, U> T retrying(HttpRequest req, BodyHandler<U> bodyHandler, Function<U, T> callback) throws IOException, InterruptedException {
		int retries = 0;
		while (true) {
			var res = client.send(req, bodyHandler);
			if (res.statusCode()/100 == 2) {
				return callback.apply(res.body());
			} else if (res.statusCode()/100 == 4) {
				System.err.println("Server returned non-transient error "+res.statusCode()+" for "+req.uri());
				return null;
			} else {
				retries++;
				if (retries > 5) {
					System.err.println("Maximum retries exceeded, giving up.");
					return null;
				} else {
					System.err.println("Server returned error "+res.statusCode()+" for "+req.uri()+" - retrying in "+retries+"s");
				}
				Thread.sleep(retries*1000);
			}
		}
	}

	public boolean hasMedia() {
		// returning true here will make the video not get updated when the sign is changed
		return false;
	}

	public void setRepeat(boolean value) {
		this.repeat = value;
		if (value && !playing && url != null) {
			play(url);
		}
	}

	public long getTime() {
		return ogv == null ? 0 : time;
	}

	public void setTime(long value) {
		if (value > time) {
			reset = true;
		}
		seekTarget = value;
		videoQueue.clear();
	}

	public Identifier getTexture() {
		return textureId;
	}

	public float getFrameRate() {
		return ogv == null ? 0 : ogv.getTheoraInfo().fps_numerator/((float)ogv.getTheoraInfo().fps_denominator);
	}

}
