import React, { PropTypes } from 'react';
import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export const TaskId = (
  <Column
    label="Task ID"
    id="taskId"
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
  />
);

export const StartedAt = (
  <Column
    label="Started At"
    id="startedAt"
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
    id="host"
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
    label="CPUs"
    id="cpus"
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

export const Actions = (
  <Column
    label=""
    id="actions"
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
