# minirest
Tiny Java REST library for Android

## Documentation

(TODO)

## Adding to your project

Copy the contents of the src folder it in to your project's src folder. 

(I'll do better eventually.)

## Examples

Reading a JSON feed:

```java
public JSONObject downloadJSON(String url) throws Exception {
    return Rest.get(url).asJSON().getJSONObject();
}
```

Reading a JSON feed with some query parameters:

```java
public JSONObject fetchWeatherJSON(String city) throws Exception {
    return Rest.get("http://api.openweathermap.org/data/2.5/forecast")
               .param("q", city)
               .param("mode", "json")
               .param("appid", "YOUR_APP_ID")
               .asJSON()
               .getJSONObject();
}
```

Sending a JSON body and receiving a JSON response, using basic authentication:

```java
public JSONObject putRecord(JSONObject record) throws Exception {
    return Rest.put(ENDPOINT + "contrived_example")
               .basicAuth(USERNAME, PASSWORD)
               .paramIfNotNull("session_id", getSessionId())
               .body(record) // Serialises to JSON automatically
               .asJSON()
               .getJSONObject();
}
```
