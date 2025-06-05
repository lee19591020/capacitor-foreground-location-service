# capacitor-foreground-location-service

A capacitor plugin for foreground location service in android and iOs

## Install

```bash
npm install capacitor-foreground-location-service
npx cap sync
```

## API

<docgen-index>

* [`setApiOptions(...)`](#setapioptions)
* [`config(...)`](#config)
* [`requestPermission()`](#requestpermission)
* [`startService()`](#startservice)
* [`stopService()`](#stopservice)
* [`addListener('locationUpdate', ...)`](#addlistenerlocationupdate-)
* [`initialize(...)`](#initialize)
* [`startUpdatingLocation()`](#startupdatinglocation)
* [`stopUpdatingLocation()`](#stopupdatinglocation)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)
* [Enums](#enums)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### setApiOptions(...)

```typescript
setApiOptions(apiOptions: CompleteOrNothing<SetApiOptions>) => Promise<OptionResponse>
```

| Param            | Type                                                                                                              |
| ---------------- | ----------------------------------------------------------------------------------------------------------------- |
| **`apiOptions`** | <code><a href="#completeornothing">CompleteOrNothing</a>&lt;<a href="#setapioptions">SetApiOptions</a>&gt;</code> |

**Returns:** <code>Promise&lt;<a href="#optionresponse">OptionResponse</a>&gt;</code>

--------------------


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


#### OptionResponse

| Prop         | Type                |
| ------------ | ------------------- |
| **`result`** | <code>string</code> |


#### SetApiOptions

| Prop               | Type                                                  |
| ------------------ | ----------------------------------------------------- |
| **`endpoint`**     | <code><a href="#endpoint">Endpoint</a></code>         |
| **`geofenceData`** | <code><a href="#geofencedata">GeofenceData</a></code> |
| **`userData`**     | <code><a href="#userdata">UserData</a></code>         |
| **`logsEndpoint`** | <code><a href="#logsendpoint">LogsEndpoint</a></code> |


#### Endpoint

| Prop           | Type                |
| -------------- | ------------------- |
| **`endPoint`** | <code>string</code> |


#### GeofenceData

| Prop               | Type                    |
| ------------------ | ----------------------- |
| **`geofenceData`** | <code>Geofence[]</code> |


#### Geofence

| Prop                      | Type                |
| ------------------------- | ------------------- |
| **`clockNumber`**         | <code>number</code> |
| **`clockDescription`**    | <code>string</code> |
| **`locationCode`**        | <code>string</code> |
| **`locationDescription`** | <code>string</code> |
| **`lat`**                 | <code>number</code> |
| **`lng`**                 | <code>number</code> |
| **`radius`**              | <code>number</code> |


#### UserData

| Prop           | Type                |
| -------------- | ------------------- |
| **`username`** | <code>string</code> |
| **`userId`**   | <code>number</code> |
| **`_token`**   | <code>string</code> |


#### LogsEndpoint

| Prop               | Type                |
| ------------------ | ------------------- |
| **`logsEndpoint`** | <code>string</code> |


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

| Prop           | Type                |
| -------------- | ------------------- |
| **`lat`**      | <code>number</code> |
| **`lng`**      | <code>number</code> |
| **`altitude`** | <code>number</code> |
| **`accuracy`** | <code>number</code> |
| **`speed`**    | <code>number</code> |
| **`bearing`**  | <code>number</code> |
| **`time`**     | <code>number</code> |


#### ForegroundLocationConfigurationIOS

| Prop                 | Type                                                              |
| -------------------- | ----------------------------------------------------------------- |
| **`accuracy`**       | <code>'high' \| 'low'</code>                                      |
| **`distanceFilter`** | <code>number</code>                                               |
| **`updateInterval`** | <code>number</code>                                               |
| **`batteryMode`**    | <code>'default' \| 'fitness' \| 'navigation' \| 'lowPower'</code> |


### Type Aliases


#### CompleteOrNothing

<code>T</code> | 


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
