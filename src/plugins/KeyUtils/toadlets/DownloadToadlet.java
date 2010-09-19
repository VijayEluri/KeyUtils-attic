/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.KeyUtils.toadlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import plugins.KeyUtils.GetResult;
import plugins.KeyUtils.KeyExplorerUtils;
import plugins.KeyUtils.KeyUtilsPlugin;
import freenet.client.DefaultMIMETypes;
import freenet.client.FetchException;
import freenet.client.Metadata;
import freenet.client.MetadataParseException;
import freenet.client.async.KeyListenerConstructionException;
import freenet.clients.http.PageNode;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.keys.FreenetURI;
import freenet.support.HTMLNode;
import freenet.support.MultiValueTable;
import freenet.support.api.HTTPRequest;
import freenet.support.io.BucketTools;
import freenet.support.plugins.helpers1.InvisibleWebInterfaceToadlet;
import freenet.support.plugins.helpers1.PluginContext;
import freenet.support.plugins.helpers1.URISanitizer;

/**
 * @author saces
 *
 */
public class DownloadToadlet extends InvisibleWebInterfaceToadlet {

	public DownloadToadlet(PluginContext context, KeyExplorerToadlet ket) {
		super(context, KeyUtilsPlugin.PLUGIN_URI, "Download", ket);
	}

	public void handleMethodGET(URI uri, HTTPRequest request, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		if (!normalizePath(request.getPath()).equals("/")) {
			sendErrorPage(ctx, 404, "Not found", "the path '"+uri+"' was not found");
			return;
		}

		List<String> errors = new LinkedList<String>();

		String key = request.getParam(Globals.PARAM_URI).trim();
		String action = request.getParam("action").trim();

		if (action.length() == 0) {
			errors.add("Parameter 'action' missing.");
		}

		if (key.length() == 0) {
			errors.add("Parameter 'key' missing.");
		}

		if (errors.size()>0) {
			makeErrorPage(ctx, errors);
			return;
		}

		if ("splitdownload".equals(action)) {
			byte[] data = doDownload(errors, key);
			if (errors.size()==0) {
				MultiValueTable<String, String> head = new MultiValueTable<String, String>();
				head.put("Content-Disposition", "attachment; filename=\"split-download\"");
				ctx.sendReplyHeaders(200, "Found", head, DefaultMIMETypes.DEFAULT_MIME_TYPE, data.length);
				ctx.writeData(data);
				return;
			} else {
				makeErrorPage(ctx, errors);
				return;
			}
		}

		errors.add("Did not understud action='"+action+"'.");
		makeErrorPage(ctx, errors);
	}

	private void makeErrorPage(ToadletContext ctx, List<String> errors) throws ToadletContextClosedException, IOException {
		PageNode page = pluginContext.pageMaker.getPageNode(KeyUtilsPlugin.PLUGIN_TITLE, ctx);
		HTMLNode outer = page.outer;
		HTMLNode contentNode = page.content;

		contentNode.addChild(createErrorBox(errors, path(), null, null));

		writeHTMLReply(ctx, 501, "OK", outer.generate());
	}

	private byte[] doDownload(List<String> errors, String key) {

		if (errors.size() > 0) {
			return null;
		}
		if (key == null || (key.trim().length() == 0)) {
			errors.add("Are you jokingly? Empty URI");
			return null;
		}
		try {
			FreenetURI furi = URISanitizer.sanitizeURI(errors, key, false, URISanitizer.Options.NOMETASTRINGS, URISanitizer.Options.SSKFORUSK);
			GetResult getresult = KeyExplorerUtils.simpleGet(pluginContext.pluginRespirator, furi);
			if (getresult.isMetaData()) {
				return KeyExplorerUtils.unrollMetadata(pluginContext.pluginRespirator, errors, Metadata.construct(getresult.getData()));
			} else {
				return BucketTools.toByteArray(getresult.getData());
			}
		} catch (MalformedURLException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (MetadataParseException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (FetchException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		} catch (KeyListenerConstructionException e) {
			errors.add(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
