#### Overview
Manages Singularity racks.

#### **POST** `/api/racks/rack/{rackId}/freeze`

Freeze a specific rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/racks/rack/{rackId}/decommission`

Begin decommissioning a specific active rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active rack ID | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **POST** `/api/racks/rack/{rackId}/activate`

Activate a decomissioning rack, canceling decomission without erasing history


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Active rackId | string |
**body**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| body | false |  | [SingularityMachineChangeRequest](#model-linkType)</a> |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/rack/{rackId}`

Retrieve the history of a given rack


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |

###### Response
[List[SingularityMachineStateHistoryUpdate]](#model-SingularityMachineStateHistoryUpdate)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **DELETE** `/api/racks/rack/{rackId}`

Remove a known rack, erasing history. This operation will cancel decomissioning of racks


###### Parameters
**path**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| rackId | true | Rack ID | string |

###### Response



###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -
#### **GET** `/api/racks/`

Retrieve the list of all known racks, optionally filtering by a particular state


###### Parameters
**query**

| Parameter | Required | Description | Data Type |
|-----------|----------|-------------|-----------|
| state | false | Optionally specify a particular state to filter racks by | string |

###### Response
[List[SingularityRack]](#model-SingularityRack)


###### Errors
| Status Code | Reason      | Response Model |
|-------------|-------------|----------------|
| - | - | - |


- - -