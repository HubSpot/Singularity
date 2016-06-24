import React, { PropTypes } from 'react';
import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export const TaskId = (
  <Column
    label="Task ID"
    id="taskId"
    key="taskId"
    cellData={
      (rowData) => rowData.taskId.id
    }
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/task/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable={true}
    className="keep-in-check"
  />
);

export const StartedAt = (
  <Column
    label="Started At"
    id="startedAt"
    key="startedAt"
    cellData={
      (rowData) => rowData.taskId.startedAt
    }
    cellRender={
      (cellData) => (
        Utils.timeStampFromNow(cellData)
      )
    }
    sortable={true}
  />
);

export const Host = (
  <Column
    label="Host"
    id="host"
    key="host"
    cellData={
      (rowData) => rowData.taskId.host
    }
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/tasks/active/all/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable={true}
  />
);

export const Rack = (
  <Column
    label="Rack"
    id="rack"
    key="rack"
    cellData={
      (rowData) => rowData.taskId.rackId
    }
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/tasks/active/all/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable={true}
  />
);

export const CPUs = (
  <Column
    label="CPUs"
    id="cpus"
    key="cpus"
    cellData={
      (rowData) => _.find(rowData.mesosTask.resources, (r) => r.name == 'cpus').scalar.value
    }
    cellRender={
      (cellData) => (
        <span>{cellData}</span>
      )
    }
    sortable={true}
  />
);

export const Memory = (
  <Column
    label="Memory"
    id="memory"
    key="memory"
    cellData={
      (rowData) => _.find(rowData.mesosTask.resources, (r) => r.name == 'mem').scalar.value
    }
    cellRender={
      (cellData) => (
        <span>{cellData} MB</span>
      )
    }
    sortable={true}
  />
);

export const ActiveActions = (
  <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellData={
      (rowData) => rowData
    }
    cellRender={(cellData) => {
        return (
          <div className="hidden-xs">
            <a><Glyphicon iconClass="remove" /></a>
            <JSONButton className="inline" object={cellData}>
              {'{ }'}
            </JSONButton>
          </div>
        );
      }
    }
  />
);

export const NextRun = (
  <Column
    label="Next Run"
    id="nextRun"
    key="nextRun"
    cellData={
      (rowData) => rowData.pendingTask.pendingTaskId.nextRunAt
    }
    cellRender={(cellData) => {
        return (
          <div>
            {Utils.timeStampFromNow(cellData)} {cellData < Date.now() ? <span className="label label-danger">OVERDUE</span> : <span className="label label-default">SCHEDULED</span>}
          </div>
        );
      }
    }
    sortable={true}
  />
);

export const PendingType = (
  <Column
    label="Pending Type"
    id="pendingType"
    key="pendingType"
    cellData={
      (rowData) => rowData.pendingTask.pendingTaskId.pendingType
    }
    cellRender={(cellData) => {
        return (
          <div>
            {Utils.humanizeText(cellData)}
          </div>
        );
      }
    }
    sortable={true}
  />
);

export const DeployId = (
  <Column
    label="Deploy ID"
    id="deployId"
    key="deployId"
    cellData={
      (rowData) => rowData.pendingTask.pendingTaskId
    }
    cellRender={(cellData) => {
        return (
          <a href={`${config.appRoot}/request/${cellData.requestId}/deploy/${cellData.deployId}`}>{cellData.deployId}</a>
        );
      }
    }
    sortable={true}
  />
);

export const ScheduledActions = (
  <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellData={
      (rowData) => rowData
    }
    cellRender={(cellData) => {
        return (
          <div className="hidden-xs">
            <a><Glyphicon iconClass="flash" /></a>
            <JSONButton className="inline" object={cellData}>
              {'{ }'}
            </JSONButton>
          </div>
        );
      }
    }
  />
);

export const ScheduledTaskId = (
  <Column
    label="Task ID"
    id="taskId"
    key="taskId"
    cellData={
      (rowData) => rowData.pendingTask.pendingTaskId.id
    }
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/task/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable={true}
    className="keep-in-check"
  />
);
