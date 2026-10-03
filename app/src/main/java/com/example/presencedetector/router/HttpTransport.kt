package com.example.presencedetector.router

/** Minimal HTTP surface used by router clients. Implementations keep cookies between calls. */
interface HttpTransport {
  /**
   * POSTs a url-encoded form and returns the response body; throws [RouterException] on failure.
   */
  fun postForm(url: String, form: Map<String, String>): String
}
