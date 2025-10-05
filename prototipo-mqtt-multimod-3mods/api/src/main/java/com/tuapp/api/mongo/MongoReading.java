package com.tuapp.api.mongo;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "readings")
public class MongoReading {
    @Id
    private String id;
    private String topic;
	private long lastTs;
    private int count;
    private List<RawEntry> raws;

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public long getLastTs() {
		return lastTs;
	}

	public void setLastTs(long lastTs) {
		this.lastTs = lastTs;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<RawEntry> getRaws() {
		return raws;
	}

	public void setRaws(List<RawEntry> raws) {
		this.raws = raws;
	}

    public static class RawEntry {
        private long ts;
        private long receivedAt;
        private Map<String,Object> payload;
        // getters/setters
		public long getTs() {
			return ts;
		}
		public void setTs(long ts) {
			this.ts = ts;
		}
		public long getReceivedAt() {
			return receivedAt;
		}
		public void setReceivedAt(long receivedAt) {
			this.receivedAt = receivedAt;
		}
		public Map<String, Object> getPayload() {
			return payload;
		}
		public void setPayload(Map<String, Object> payload) {
			this.payload = payload;
		}
    }
}
