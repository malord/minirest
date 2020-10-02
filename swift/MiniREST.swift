import Foundation

/// Simple REST client with asynchronous and synchronous methods.
class MiniREST: NSObject, URLSessionDelegate {
    enum Method: String {
        case GET = "GET"
        case POST = "POST"
        case PUT = "PUT"
        case PATCH = "PATCH"
        case DELETE = "DELETE"
    }

    enum MiniRESTError: Error {
        /// run() wasn't called
        case requestNotMade
        case invalidURL
        case responseCode(code: Int)
        case invalidEncoding
        case noResponseData
        case invalidResponse
    }

    private var method: Method
    private var url: URLComponents
    private var headers = Dictionary<String, String>()

    var timeout: TimeInterval
    var body: Data?
    var haveRun = false

    private(set) var error: Error?

    private(set) var responseCode = 400
    private(set) var responseHeaders: [AnyHashable: Any]?
    private(set) var responseData: Data?

    var throwOnResponseCode = false
    var forceUncached = false

    init(_ method: Method, url: String, timeout: TimeInterval = 60) throws {
        self.method = method
        guard let url = URLComponents(string: url) else {
            throw MiniRESTError.invalidURL
        }
        self.url = url
        self.timeout = timeout
    }

    /// Set a header
    func header(name: String, value: String) {
        headers[name] = value
    }

    /// Append or update a query string item
    func query(name: String, value: String?) {
        var items: [URLQueryItem] = url.queryItems == nil ? [] : url.queryItems!
        var found: Int?
        for (index, item) in items.enumerated() {
            if item.name == name {
                found = index
                break
            }
        }

        let newItem = URLQueryItem(name: name, value: value)

        if let found = found {
            items[found] = newItem
        } else {
            items.append(newItem)
        }

        url.queryItems = items
    }

    /// Append a query string item, even if there's already an item with the same name
    func appendQuery(name: String, value: String?) {
        var items: [URLQueryItem] = url.queryItems == nil ? [] : url.queryItems!
        items.append(URLQueryItem(name: name, value: value))
        url.queryItems = items
    }

    /// Returns nil if the item is present with no value, or is not present.
    func query(name: String) -> String? {
        if let items = url.queryItems {
            for item in items {
                if item.name == name {
                    return item.value
                }
            }
        }

        return nil
    }
}

//
// MARK: Asynchronous API
//

extension MiniREST {

    /// Runs the request asynchronously and converts the response to a `String`
    func string(then: @escaping (_ error: Error?, _ string: String?, _ minirest: MiniREST) -> Void) {
        run { (error, _) in
            if let error = error {
                then(error, nil, self)
            } else {
                do {
                    then(nil, try self.string(), self)
                } catch {
                    then(error, nil, self)
                }
            }
        }
    }

    /// Runs the request asynchronously and converts the JSON response to a `Dictionary<String, Any>` or `Array<Any>`
    func json(then: @escaping (_ error: Error?, _ object: Any?, _ minirest: MiniREST) -> Void) {
        run { (error, _) in
            if let error = error {
                then(error, nil, self)
            } else {
                do {
                    // I could move the deserialization to a thread here
                    guard let data = self.responseData else {
                        throw MiniRESTError.noResponseData
                    }

                    let json = try JSONSerialization.jsonObject(with: data, options: [])

                    then(nil, json, self)
                } catch {
                    then(error, nil, self)
                }
            }
        }
    }

    /// Runs the request asynchronously and converts the JSON response to a `Dictionary<String, Any>`
    func dictionary(then: @escaping (_ error: Error?, _ object: [String: Any]?, _ minirest: MiniREST) -> Void) {
        run { (error, _) in
            if let error = error {
                then(error, nil, self)
            } else {
                do {
                    then(nil, try self.dictionary(), self)
                } catch {
                    then(error, nil, self)
                }
            }
        }
    }

    /// Runs the request asynchronously then invokes the supplied callback
    func run(then: @escaping (_ error: Error?, _ minirest: MiniREST) -> Void) {
        haveRun = true

        let config = URLSessionConfiguration.default
        let session: URLSession = URLSession(
            configuration: config,
            delegate: self,
            delegateQueue: OperationQueue())

        guard let url = url.url else {
            then(MiniRESTError.invalidURL, self)
            return
        }

        var request = URLRequest(
            url: url,
            cachePolicy: forceUncached ? .reloadIgnoringLocalCacheData : .useProtocolCachePolicy,
            timeoutInterval: timeout)

        request.httpMethod = method.rawValue

        for (name, value) in headers {
            request.setValue(value, forHTTPHeaderField: name)
        }

        if let body = body {
            request.httpBody = body
            request.setValue("\(body.count)", forHTTPHeaderField: "Content-Length")
        }

        if forceUncached {
            request.setValue("no-cache, no-store, must-revalidate", forHTTPHeaderField: "Cache-Control")
            request.setValue("no-cache", forHTTPHeaderField: "Pragma")
            request.setValue("0", forHTTPHeaderField: "Expires")
        }

        self.error = nil

        let task = session.dataTask(with: request) { (data, response, error) in
            if let error = error {
                self.error = error
                self.responseCode = 400
                then(error, self)
                return
            }

            if let httpResponse = response as? HTTPURLResponse {
                self.responseCode = httpResponse.statusCode
                self.responseHeaders = httpResponse.allHeaderFields
            }

            self.responseData = data

            if self.throwOnResponseCode && self.responseCode / 100 != 2 {
                then(MiniRESTError.responseCode(code: self.responseCode), self)
            } else {
                then(nil, self)
            }
        }

        task.resume()
    }
}

//
// MARK: Synchronous API
//

extension MiniREST {
    
    /// Runs the request synchronously and returns the response code
    func run() throws -> Int {
        let semaphore = DispatchSemaphore(value: 0)

        var throwError: Error?

        run { (error, _) in
            throwError = error
            semaphore.signal()
        }

        semaphore.wait()

        if let error = throwError {
            throw error
        }

        return self.responseCode
    }

    private func runShouldHaveBeenCalled() throws {
        if !haveRun {
            throw MiniRESTError.requestNotMade
        }
    }

    /// Converts the response to a `String`
    func string() throws -> String {
        try runShouldHaveBeenCalled()

        guard let data = responseData else {
            throw MiniRESTError.noResponseData
        }

        guard let string = String(data: data, encoding: .utf8) else {
            throw MiniRESTError.invalidEncoding
        }

        return string
    }

    /// Converts  the JSON response to a `Dictionary<String, Any>` or `Array<Any>`
    func json() throws -> Any {
        try runShouldHaveBeenCalled()

        guard let data = responseData else {
            throw MiniRESTError.noResponseData
        }

        return try JSONSerialization.jsonObject(with: data, options: [])
    }

    /// Converts the JSON response to a `Dictionary<String, Any>`
    func dictionary() throws -> [String: Any] {
        let json = try self.json()

        guard let dictionary = json as? [String: Any] else {
            throw MiniRESTError.invalidResponse
        }

        return dictionary
    }
}
