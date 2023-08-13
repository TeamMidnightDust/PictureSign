package eu.midnightdust.picturesign.ogv;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.Identifier;

public class VideoManager {
	
	private final Map<Identifier, Video> videos = new HashMap<>();
	
	public void update() {
		videos.values().forEach(Video::update);
	}

	public void closePlayer(Identifier id) {
		var v = videos.remove(id);
		if (v != null) v.destroy();
	}

	public Video getOrCreate(Identifier id) {
		return videos.computeIfAbsent(id, Video::new);
	}

}
