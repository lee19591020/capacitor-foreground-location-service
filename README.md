# capacitor-foreground-location-service

A capacitor plugin for foreground location service in android and iOs

## Install

```bash
npm install capacitor-foreground-location-service
npx cap sync
```

## API

<docgen-index>

* [`config(...)`](#config)
* [`requestPermission()`](#requestpermission)
* [`startService()`](#startservice)
* [`stopService()`](#stopservice)
* [`addListener('locationUpdate', ...)`](#addlistenerlocationupdate-)
* [`initialize(...)`](#initialize)
* [`startUpdatingLocation()`](#startupdatinglocation)
* [`stopUpdatingLocation()`](#stopupdatinglocation)
* [Interfaces](#interfaces)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### config(...)

```typescript
config(config: ForegroundLocationConfiguration) => Promise<void>
```

| Param        | Type                                                                                        |
| ------------ | ------------------------------------------------------------------------------------------- |
| **`config`** | <code><a href="#foregroundlocationconfiguration">ForegroundLocationConfiguration</a></code> |

--------------------


### requestPermission()

```typescript
requestPermission() => Promise<PermissionResponse>
```

**Returns:** <code>Promise&lt;<a href="#permissionresponse">PermissionResponse</a>&gt;</code>

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


### initialize(...)

```typescript
initialize(config: ForegroundLocationConfigurationIOS) => Promise<void>
```

| Param        | Type                                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------- |
| **`config`** | <code><a href="#foregroundlocationconfigurationios">ForegroundLocationConfigurationIOS</a></code> |

--------------------


### startUpdatingLocation()

```typescript
startUpdatingLocation() => Promise<void>
```

--------------------


### stopUpdatingLocation()

```typescript
stopUpdatingLocation() => Promise<void>
```

--------------------


### Interfaces


#### ForegroundLocationConfiguration

| Prop                         | Type                                                                      |
| ---------------------------- | ------------------------------------------------------------------------- |
| **`interval`**               | <code>number</code>                                                       |
| **`distanceFilter`**         | <code>number</code>                                                       |
| **`notificationTitle`**      | <code>string</code>                                                       |
| **`notificationMessage`**    | <code>string</code>                                                       |
| **`notificationImportance`** | <code><a href="#notificationimportance">NotificationImportance</a></code> |
| **`notificationChannelId`**  | <code>number</code>                                                       |


#### PermissionResponse

| Prop          | Type                 |
| ------------- | -------------------- |
| **`granted`** | <code>boolean</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### ForegroundLocation

| Prop      | Type                |
| --------- | ------------------- |
| **`lat`** | <code>number</code> |
| **`lng`** | <code>number</code> |


#### ForegroundLocationConfigurationIOS

| Prop                 | Type                                                              |
| -------------------- | ----------------------------------------------------------------- |
| **`accuracy`**       | <code>'high' \| 'low'</code>                                      |
| **`distanceFilter`** | <code>number</code>                                               |
| **`updateInterval`** | <code>number</code>                                               |
| **`batteryMode`**    | <code>'default' \| 'fitness' \| 'navigation' \| 'lowPower'</code> |


### Enums


#### NotificationImportance

| Members       | Value          |
| ------------- | -------------- |
| **`MIN`**     | <code>1</code> |
| **`LWO`**     | <code>2</code> |
| **`DEFAULT`** | <code>3</code> |
| **`HIGH`**    | <code>4</code> |
| **`MAX`**     | <code>5</code> |

</docgen-api>
