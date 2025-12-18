# Path Exclusion Filter

## Overview

The `PathExclusionFilter` is a servlet filter that prevents specific paths from being handled by the JAX-RS application. This is useful when you want to reserve certain URL paths for other servlets or handlers in the OSGi container.

## How It Works

The filter intercepts all requests (`/*`) and checks if the request path starts with any of the configured excluded paths. If a match is found, the filter returns a 404 error immediately, preventing the JAX-RS application from processing the request.

## Configuration

The filter can be configured using OSGi Configuration Admin. The following properties are available:

### Excluded Paths
- **Property**: `excludedPaths`
- **Type**: String array
- **Default**: `["/system", "/admin", "/bin", "/content"]`
- **Description**: Paths that should NOT be handled by the JAX-RS application. Any request starting with these paths will receive a 404 response.

### Enabled
- **Property**: `enabled`
- **Type**: Boolean
- **Default**: `true`
- **Description**: Enable or disable the path exclusion filter

## Configuration Examples

### Using Apache Felix Web Console

1. Navigate to OSGi Configuration Admin
2. Find "Caravan Rhyme Example Service - Path Exclusion Filter"
3. Add or modify the excluded paths
4. Save the configuration

### Using OSGi Config File

Create a file named `io.wcm.caravan.rhyme.osgi.sampleservice.impl.jaxrs.PathExclusionFilter.cfg` with content:

```properties
excludedPaths=["/system","/admin","/bin","/content","/custom"]
enabled=true
```

Or in JSON format (`.config` file):

```json
{
  "excludedPaths": [
    "/system",
    "/admin",
    "/bin",
    "/content",
    "/custom"
  ],
  "enabled": true
}
```

## Usage Examples

### Example 1: Exclude Admin Paths
If you configure excludedPaths to include `"/admin"`, then:
- Request to `/admin/users` → 404 error (excluded)
- Request to `/api/users` → Handled by JAX-RS application
- Request to `/` → Handled by JAX-RS application

### Example 2: Exclude Multiple Paths
Configure excludedPaths: `["/system", "/bin", "/content"]`
- Request to `/system/console` → 404 error
- Request to `/bin/export` → 404 error
- Request to `/content/page` → 404 error
- Request to `/caching` → Handled by JAX-RS application

## Technical Details

- **Filter Pattern**: `/*` (all requests)
- **Filter Priority**: Applied before JAX-RS application processing
- **Context Selection**: Applies to all servlet contexts
- **Thread Safety**: The filter is thread-safe and can handle concurrent requests

## Disabling the Filter

To completely disable the filter, set `enabled=false` in the configuration. When disabled, all requests will be processed by the JAX-RS application.

## Notes

- The filter performs prefix matching, so `/admin` will match `/admin`, `/admin/users`, `/admin/settings`, etc.
- The filter removes the servlet context path before matching, ensuring it works correctly regardless of the deployment context
- Empty or null paths in the configuration are ignored
