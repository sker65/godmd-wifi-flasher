package com.rinke.solutions.godmd.flash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.SwingWorker;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

@SuppressWarnings("deprecation")
class Uploader extends SwingWorker<Pair<Integer, String>, Pair<Integer, String>> implements ProgressListener {

	/**
	 * 
	 */
	private Pair<Integer,String> pair(int i, String msg) {
		return new Pair<Integer, String>(i, msg);
	}
	
	@Override
	public void transferred(long transferedBytes) {
		int val = (int) (((float) transferedBytes / (float) size) * 80.0) + 15;
		publish(new Pair<Integer, String>(val, "uploading " + line + " " + transferedBytes + "..."));
	}

	long size = 1;
	String line;
	String hostname;
	@SuppressWarnings("deprecation")
	HttpClient client = new DefaultHttpClient();
	private boolean success;
	private String firmware;
	private Progress progress;

	public Uploader(String hostname, String firmware) {
		super();
		this.hostname = hostname;
		this.firmware = firmware;
		client.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_0);
	}

	@Override
	protected Pair<Integer, String> doInBackground() throws Exception {
		HttpGet request = new HttpGet("http://" + hostname + "/flash/next");
		try {
			publish(pair(1, "connecting " + hostname + " ..."));
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			line = rd.readLine();
			publish(pair(10, "about to flash " + line + " ..."));
			// System.out.println(line);
			Thread.sleep(500);
			rd.close();

			HttpPost post = new HttpPost("http://" + hostname + "/flash/upload");
			post.setProtocolVersion(HttpVersion.HTTP_1_0); // suppress
															// chunked
			RequestConfig config = RequestConfig.custom().setContentCompressionEnabled(false).build();
			post.setConfig(config);
			ZipFile zf = new ZipFile(firmware);
			ZipEntry entry = zf.getEntry(line);
			size = entry.getSize();
			InputStream instream = zf.getInputStream(zf.getEntry(line));
			InputStreamEntity isEntity = new InputStreamEntity(instream, size);
			isEntity.setChunked(false);
			CountingHttpEntity countingHttpEntity = new CountingHttpEntity(isEntity, this);
			post.setEntity(countingHttpEntity);
			publish(pair(15, "uploading " + line + " ..."));
			response = client.execute(post);
			instream.close();
			EntityUtils.consumeQuietly(response.getEntity());

			if (response.getStatusLine().getStatusCode() == 200) {
				// System.out.println("all good");
				// trigger reboot
				publish(pair(86, "upload complete waiting for reboot ..."));
				Thread.sleep(500);
				HttpGet reboot = new HttpGet("http://" + hostname + "/flash/reboot");
				response = client.execute(reboot);
				EntityUtils.consumeQuietly(response.getEntity());
				Thread.sleep(2500);
				if (response.getStatusLine().getStatusCode() == 200) {
					String newFirmware = waitForReboot();
					if (line.equals("user1.bin") && "user2.bin".equals(newFirmware)) {
						success();
					}
					if (line.equals("user2.bin") && "user1.bin".equals(newFirmware)) {
						success();
					}
				}
			}
			zf.close();

		} catch (IOException e) {
			publish(pair(100, e.getMessage()));
			return pair(99, e.getMessage());
		}
		publish(success ? pair(100, "Success") : pair(100, "Error on Reboot"));
		return null;
	}

	private void success() {
		success = true;
		publish(pair(100, "Reboot successful"));
	}

	private String waitForReboot() {
		int count = 0;
		long start = System.currentTimeMillis();
		while (count < 80 && System.currentTimeMillis() - start < 60000) {
			HttpGet request = new HttpGet("http://" + hostname + "/flash/next");
			RequestConfig config = RequestConfig.custom().setContentCompressionEnabled(false).setConnectionRequestTimeout(400).setConnectTimeout(200)
					.build();
			request.setConfig(config);
			try {
				HttpResponse response = client.execute(request);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				String res = rd.readLine();
				rd.close();
				return res;
			} catch (IOException e) {
			}
			count++;
			publish(pair((int) (85 + (count / 80.0) * 15), "upload complete waiting for reboot ..."));
		}
		return null;
	}

	@Override
	protected void process(List<Pair<Integer, String>> chunks) {
		for (Pair<Integer, String> pair : chunks) {
			progress.setValue(pair.left);
			progress.setStatus(pair.right);
			if (pair.left == 100) {
				progress.setButtonText("Close");
			}
		}
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
	}

}