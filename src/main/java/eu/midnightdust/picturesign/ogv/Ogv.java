package eu.midnightdust.picturesign.ogv;

import eu.midnightdust.picturesign.ogv.jheora.TheoraComment;
import eu.midnightdust.picturesign.ogv.jheora.TheoraInfo;
import eu.midnightdust.picturesign.ogv.jheora.TheoraState;
import eu.midnightdust.picturesign.ogv.jheora.YUVBuffer;
import eu.midnightdust.picturesign.ogv.jogg.OggPacket;
import eu.midnightdust.picturesign.ogv.jogg.OggPage;
import eu.midnightdust.picturesign.ogv.jogg.OggStreamState;
import eu.midnightdust.picturesign.ogv.jogg.OggSyncState;
import eu.midnightdust.picturesign.ogv.jorbis.VorbisBlock;
import eu.midnightdust.picturesign.ogv.jorbis.VorbisDspState;
import eu.midnightdust.picturesign.ogv.jorbis.VorbisComment;
import eu.midnightdust.picturesign.ogv.jorbis.VorbisInfo;

import java.io.IOException;
import java.io.InputStream;

public class Ogv {
	public record VideoFrame(int width, int height, int[] rgba, long durationNs) {}
	
	private final InputStream in;

	public Ogv(InputStream in) throws IOException {
		this.in = in;
		
		initialize();
	}
	
	public TheoraInfo getTheoraInfo() {
		return ti;
	}
	
	public VorbisInfo getVorbisInfo() {
		return vi;
	}

	private final OggPacket op = new OggPacket();
	private final OggSyncState oy = new OggSyncState();
	private final OggPage og = new OggPage();
	private OggStreamState vo = new OggStreamState();
	private OggStreamState to = new OggStreamState();
	private final TheoraInfo ti = new TheoraInfo();
	private final TheoraComment tc = new TheoraComment();
	private final TheoraState td = new TheoraState();
	private final VorbisInfo vi = new VorbisInfo();
	private final VorbisDspState vd = new VorbisDspState();
	private final VorbisBlock vb = new VorbisBlock(this.vd);
	private VorbisComment vc = new VorbisComment();
	private int theora_p = 0;
	private int vorbis_p = 0;
	private int stateflag = 0;
	private int videobuf_ready = 0;
	private int audiobuf_fill = 0;
	private int audiobuf_ready = 0;
	private short[] audiobuf = new short[12000];
	private int audiofd_fragsize = 24000;
	private boolean end = false;
	
	private long granule = 0;
	private VideoFrame video;
	private short[] audio;
	
	public long getGranule() {
		return granule;
	}
	
	public VideoFrame getVideo() {
		var v = video;
		video = null;
		return v;
	}
	
	public short[] getAudio() {
		var a = audio;
		audio = null;
		return a;
	}
	
	public boolean isEnd() {
		return end;
	}

	private int buffer_data() throws IOException {
		int fill = oy.buffer(4096);
		byte[] buffer2 = oy.data;
		int bytes = in.read(buffer2, fill, 4096);
		if (bytes < 0) {
			return bytes;
		} else {
			oy.wrote(bytes);
			return bytes;
		}
	}

	void video_write() {
		YUVBuffer yuv = new YUVBuffer();
		this.td.decodeYUVout(yuv);
		int[] pixels = yuv.produce();
		video = new VideoFrame(yuv.y_width, yuv.y_height, pixels, (ti.fps_denominator*1_000_000_000L)/ti.fps_numerator);
	}

	void audio_write() {
		audio = audiobuf.clone();
	}

	int queue_page(OggPage page) {
		if (this.theora_p != 0) {
			this.to.pagein(this.og);
		}

		if (this.vorbis_p != 0) {
			this.vo.pagein(this.og);
		}

		return 0;
	}
	
	private void initialize() throws IOException {
		this.oy.init();
		this.vi.init();
		this.vc.init();

		while (this.stateflag == 0) {
			int read = this.buffer_data();
			if (read <= 0) {
				break;
			}

			while (this.oy.pageout(this.og) > 0) {
				OggStreamState test = new OggStreamState();
				if (this.og.bos() == 0) {
					this.queue_page(this.og);
					this.stateflag = 1;
					break;
				}

				test.init(this.og.serialno());
				test.pagein(this.og);
				test.packetout(op);
				if (this.theora_p == 0 && this.ti.decodeHeader(this.tc, op) >= 0) {
					this.to = test;
					this.theora_p = 1;
				} else if (this.vorbis_p == 0 && this.vi.synthesis_headerin(this.vc, op) >= 0) {
					this.vo = test;
					this.vorbis_p = 1;
				} else {
					test.clear();
				}
			}
		}

		while (this.theora_p != 0 && this.theora_p < 3 || this.vorbis_p != 0 && this.vorbis_p < 3) {
			int err;
			if (this.theora_p != 0 && this.theora_p < 3 && (err = this.to.packetout(op)) != 0) {
				if (err < 0) {
					System.err.printf("Error parsing first Theora packet; corrupt stream? error %d\n", err);
					end = true;
					return;
				}

				if ((err = this.ti.decodeHeader(this.tc, op)) != 0) {
					System.err.printf("Error parsing Theora stream headers; corrupt stream? error %d\n", err);
					end = true;
					return;//		if (this.vorbis_p != 0) {
//					this.vo.clear();
//					this.vb.clear();
//					this.vd.clear();
//					this.vi.clear();
//				}
		//
//				if (this.theora_p != 0) {
//					this.to.clear();
//					this.td.clear();
//					this.ti.clear();
//				}
				}

				++this.theora_p;
				if (this.theora_p != 3) {
					continue;
				}
			}

			while (true) {
				if (this.vorbis_p != 0 && this.vorbis_p < 3 && (err = this.vo.packetout(op)) != 0) {
					if (err < 0) {
						System.err.printf("Error parsing Vorbis stream headers; corrupt stream? error %d\n", err);
						end = true;
						return;
					}

					if ((err = this.vi.synthesis_headerin(this.vc, op)) != 0) {
						System.err.printf("Error parsing Vorbis stream headers; corrupt stream? error %d\n", err);
						end = true;
						return;
					}

					++this.vorbis_p;
					if (this.vorbis_p != 3) {
						continue;
					}
				}

				if (this.oy.pageout(this.og) > 0) {
					this.queue_page(this.og);
				} else {
					int ret2 = this.buffer_data();
					if (ret2 <= 0) {
						System.err.print("End of file while searching for codec headers.\n");
						end = true;
						return;
					}
				}
				break;
			}
		}

		if (this.theora_p != 0) {
			this.td.decodeInit(this.ti);
//			System.out
//					.printf(
//							"Ogg logical stream %x is Theora %dx%d %.02f fps",
//							getSerialNo(this.to),
//							this.ti.width,
//							this.ti.height,
//							(double) this.ti.fps_numerator / (double) this.ti.fps_denominator);
//			if (this.ti.width != this.ti.frame_width || this.ti.height != this.ti.frame_height) {
//				System.out
//						.printf("  Frame content is %dx%d with offset (%d,%d).\n", this.ti.frame_width, this.ti.frame_height, this.ti.offset_x, this.ti.offset_y);
//			}
		} else {
			this.ti.clear();
		}

		if (this.vorbis_p != 0) {
			this.vd.synthesis_init(this.vi);
			this.vb.init(this.vd);
//			System.out.printf("Ogg logical stream %x is Vorbis %d channel %d Hz audio.\n", getSerialNo(this.vo), this.vi.channels, this.vi.rate);
		} else {
			this.vi.clear();
		}
		
		this.stateflag = 0;
	}

	public void step() throws IOException {
		if (this.vorbis_p != 0 && this.audiobuf_ready == 0) {
			float[][][] pcm = new float[1][][];
			int[] index = new int[this.vi.channels];
			int ret;
			if ((ret = this.vd.synthesis_pcmout(pcm, index)) > 0) {
				float[][] floatArrays = pcm[0];
				int count = this.audiobuf_fill / 2;
				int maxsamples = (this.audiofd_fragsize - this.audiobuf_fill) / 2 / this.vi.channels;

				int i;
				for (i = 0; i < ret && i < maxsamples; ++i) {
					for (int j = 0; j < this.vi.channels; ++j) {
						int val = Math.round(floatArrays[j][index[j] + i] * 32767.0F);
						if (val > 32767) {
							val = 32767;
						}

						if (val < -32768) {
							val = -32768;
						}

						this.audiobuf[count++] = (short) val;
					}
				}

				
				this.vd.synthesis_read(i);
				this.audiobuf_fill += i * this.vi.channels * 2;
				if (this.audiobuf_fill == this.audiofd_fragsize) {
					this.audiobuf_ready = 1;
				}
				return;
			}

			if (this.vo.packetout(op) > 0) {
				if (this.vb.synthesis(op) == 0) {
					this.vd.synthesis_blockin(this.vb);
				}
				return;
			}
		}

		while (this.theora_p != 0 && this.videobuf_ready == 0 && this.to.packetout(op) > 0) {
			this.td.decodePacketin(op);
			this.videobuf_ready = 1;
		}

		if (this.videobuf_ready == 0 || this.audiobuf_ready == 0) {
			int bytes = this.buffer_data();
			if (bytes < 0) {
				while (this.oy.pageout(this.og) > 0) {
					this.queue_page(this.og);
				}

				if (this.videobuf_ready == 0 && this.audiobuf_ready == 0) {
					close();
					return;
				}
			}

			while (this.oy.pageout(this.og) > 0) {
				this.queue_page(this.og);
			}
		}
		
		if (this.stateflag != 0 && this.audiobuf_ready != 0) {
			this.audio_write();
			this.audiobuf_fill = 0;
			this.audiobuf_ready = 0;
		}

		if (this.stateflag != 0 && this.videobuf_ready != 0) {
			this.video_write();
			this.videobuf_ready = 0;
		}

		if ((this.theora_p == 0 || this.videobuf_ready != 0) && (this.vorbis_p == 0 || this.audiobuf_ready != 0)) {
			this.stateflag = 1;
		}
		
		granule = op.granulepos;
	}

	public void close() throws IOException {
//		if (this.vorbis_p != 0) {
//			this.vo.clear();
//			this.vb.clear();
//			this.vd.clear();
//			this.vi.clear();
//		}
//
//		if (this.theora_p != 0) {
//			this.to.clear();
//			this.td.clear();
//			this.ti.clear();
//		}

//		this.oy.clear();
		in.close();
		end = true;
	}
}
