package org.wordpress.android.support

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import org.wordpress.android.util.ErrorManagedWebViewClient

class SupportWebViewClient(
    private val listener: SupportWebViewClientListener,
    private val assetLoader: WebViewAssetLoader
) : ErrorManagedWebViewClient(listener) {
    interface SupportWebViewClientListener : ErrorManagedWebViewClientListener {
        fun onChatSessionClosed()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    // to support API < 21
    override fun shouldInterceptRequest(
        view: WebView,
        url: String
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(Uri.parse(url))
    }
}


