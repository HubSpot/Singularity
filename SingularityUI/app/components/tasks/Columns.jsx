import React from 'react';
import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import KillTaskButton from '../tasks/KillTaskButton';
import RunNowButton from '../requests/RunNowButton';

export const TaskId = (
  <Column
    label="Task ID"
    id="taskId"
    key="taskId"
    cellData={
      (rowData) => (rowData.taskId ? rowData.taskId.id : rowData.id)
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
      (rowData) => (rowData.taskId ? rowData.taskId.startedAt : rowData.startedAt)
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
      (rowData) => (rowData.taskId ? rowData.taskId.host : rowData.host)
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
      (rowData) => (rowData.taskId ? rowData.taskId.rackId : rowData.rackId)
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
      (rowData) => _.find(rowData.mesosTask.resources, (r) => r.name === 'cpus').scalar.value
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
      (rowData) => _.find(rowData.mesosTask.resources, (r) => r.name === 'mem').scalar.value
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
    cellRender={(cellData) => (
      <div className="hidden-xs">
        <KillTaskButton taskId={cellData.taskId.id} />
        <JSONButton className="inline" object={cellData}>
          {'{ }'}
        </JSONButton>
      </div>
    )}
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
      let label = <span className="label label-default">SCHEDULED</span>;
      if (Utils.timestampWithinSeconds(cellData, config.pendingWithinSeconds)) {
        label = <span className="label label-info">PENDING</span>;
      } else if (cellData < Date.now() - config.pendingWithinSeconds * 1000) {
        label = <span className="label label-danger">OVERDUE</span>;
      }
      return (
        <div>
          {Utils.timeStampFromNow(cellData)} {label}
        </div>
      );
    }}
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
    cellRender={(cellData) => (
      <div>
        {Utils.humanizeText(cellData)}
      </div>
    )}
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
    sortData={(cellData) => cellData.deployId}
    cellRender={(cellData) => (
      <a href={`${config.appRoot}/request/${cellData.requestId}/deploy/${cellData.deployId}`}>{cellData.deployId}</a>
    )}
    sortable={true}
  />
);

export const ScheduledActions = (
  <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellRender={(cellData) => (
      <div className="hidden-xs">
        <RunNowButton requestId={cellData.pendingTask.pendingTaskId.requestId} />
        <JSONButton className="inline" object={cellData}>
          {'{ }'}
        </JSONButton>
      </div>
    )}
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
    cellRender={(cellData) => (
      <a href={`${config.appRoot}/task/${cellData}`}>
        {cellData}
      </a>
    )}
    sortable={true}
    className="keep-in-check"
  />
);

export const CleanupType = (
  <Column
    label="Cleanup Type"
    id="cleanupType"
    key="cleanupType"
    cellData={
      (rowData) => rowData.cleanupType
    }
    cellRender={
      (cellData) => (
        Utils.humanizeText(cellData)
      )
    }
    sortable={true}
  />
);

export const JSONAction = (
  <Column
    label=""
    id="jsonAction"
    key="jsonAction"
    className="actions-column"
    cellRender={(cellData) => (
      <div className="hidden-xs">
        <JSONButton className="inline" object={cellData}>
          {'{ }'}
        </JSONButton>
      </div>
    )}
  />
);

export const InstanceNumber = (
  <Column
    label="Instance Number"
    id="instanceNo"
    key="instanceNo"
    cellData={
      (rowData) => rowData.instanceNo
    }
    sortable={true}
  />
);
