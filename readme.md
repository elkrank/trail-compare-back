# Trail Compare Backend

Backend for Trail Compare.

## Admin race creation API

`POST /api/admin/races` accepts either JSON or multipart requests.

### JSON-only race creation

Send `Content-Type: application/json` with a `RaceRequest` body when creating a race without a GPX file.

### Multipart race creation with optional GPX

Send `Content-Type: multipart/form-data` with these exact parts:

| Part name | Required | Content type | Description |
| --- | --- | --- | --- |
| `race` | yes | `application/json` | Serialized `RaceRequest` payload. |
| `gpx` | no | `application/gpx+xml` | Optional `.gpx` track file imported during race creation. |

The file part name for this endpoint is officially `gpx`. The legacy `file` part name is rejected for `POST /api/admin/races` to keep the contract unambiguous. The standalone endpoint `POST /api/admin/races/{id}/gpx` still uses the `file` part name.

Frontend/admin clients should build the request with `FormData` as follows:

```js
const formData = new FormData();
formData.append(
  'race',
  new Blob([JSON.stringify(racePayload)], { type: 'application/json' }),
  'race.json',
);

if (gpxFile) {
  formData.append('gpx', gpxFile);
}

await fetch('/api/admin/races', {
  method: 'POST',
  body: formData,
});
```
