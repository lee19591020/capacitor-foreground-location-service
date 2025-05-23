# capacitor-foreground-location-service

A capacitor plugin for foreground location service in android and iOs

## Install

```bash
npm install capacitor-foreground-location-service
npx cap sync
```

## API

<docgen-index>

* [`requestPermission()`](#requestpermission)
* [`startService()`](#startservice)
* [`stopService()`](#stopservice)
* [`addListener('locationUpdate', ...)`](#addlistenerlocationupdate-)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### requestPermission()

```typescript
requestPermission() => Promise<boolean>
```

**Returns:** <code>Promise&lt;boolean&gt;</code>

--------------------


### startService()

```typescript
startService() => Promise<void>
```

--------------------


### stopService()

```typescript
stopService() => Promise<void>
```

--------------------


### addListener('locationUpdate', ...)

```typescript
addListener(eventName: 'locationUpdate', listenerFunc: (location: ForegroundLocation) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

| Param              | Type                                                                                     |
| ------------------ | ---------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'locationUpdate'</code>                                                            |
| **`listenerFunc`** | <code>(location: <a href="#foregroundlocation">ForegroundLocation</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### ForegroundLocation

| Prop      | Type                |
| --------- | ------------------- |
| **`lat`** | <code>number</code> |
| **`lng`** | <code>number</code> |

</docgen-api>
