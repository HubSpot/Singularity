import React from 'react';
import { Link } from 'react-router';
import { Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Column from '../common/table/Column';
import classNames from 'classnames';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';
import KillTaskButton from '../common/modalButtons/KillTaskButton';
import RunNowButton from '../common/modalButtons/RunNowButton';

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
        <Link to={`task/${cellData}`}>
          {cellData}
        </Link>
      )
    }
    sortable={true}
  />
);

export const TaskIdShortened = (
  <Column
    label="Task ID"
    id="taskIdShort"
    key="taskIdShort"
    cellData={
      (rowData) => (rowData.taskId ? rowData.taskId.id : rowData.id)
    }
    cellRender={
      (cellData) => (
        <Link to={`task/${cellData}`}>
          {cellData}
        </Link>
      )
    }
    sortable={true}
    className="keep-in-check"
  />
);

export const LastTaskState = (
  <Column
    label="Status"
    id="lastTaskState"
    key="lastTaskState"
    cellData={
      (rowData) => rowData.lastTaskState
    }
    cellRender={
      (cellData) => {
        const className = classNames(
          'label',
          `label-${Utils.getLabelClassFromTaskState(cellData)}`
        );
        return (
          <span className={className}>
            {Utils.humanizeText(cellData)}
          </span>
        );
      }
    }
    sortable={true}
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
        Utils.timestampFromNow(cellData)
      )
    }
    sortable={true}
  />
);

export const UpdatedAt = (
  <Column
    label="Updated At"
    id="updatedAt"
    key="updatedAt"
    cellData={
      (rowData) => rowData.updatedAt
    }
    cellRender={
      (cellData) => (
        Utils.timestampFromNow(cellData)
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
      (rowData) => (Utils.humanizeSlaveHostName(rowData.host ? rowData.host : rowData.taskId.host))
    }
    cellRender={
      (cellData) => (
        <Link to={`tasks/active/all/${cellData}`}>
          {cellData}
        </Link>
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
        <Link to={`tasks/active/all/${cellData}`}>
          {cellData}
        </Link>
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
      (rowData) => _.find(rowData.mesosTask.resources, (resource) => resource.name === 'cpus').scalar.value
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
      (rowData) => _.find(rowData.mesosTask.resources, (resource) => resource.name === 'mem').scalar.value
    }
    cellRender={
      (cellData) => (
        <span>{cellData} MB</span>
      )
    }
    sortable={true}
  />
);

export const Disk = (
  <Column
    label="Disk"
    id="disk"
    key="disk"
    cellData={
      (rowData) => {
        const disk = _.find(rowData.mesosTask.resources, (resource) => resource.name === 'disk');
        if (disk) {
          return disk.scalar.value;
        }
        return null;
      }
    }
    cellRender={
      (cellData) => (cellData &&
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
        <KillTaskButton
          taskId={cellData.taskId.id}
          shouldShowWaitForReplacementTask={Utils.isIn(cellData.taskRequest.request.requestType, ['SERVICE', 'WORKER'])}
        />
        <JSONButton className="inline" object={cellData} showOverlay={true}>
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
      let label = <span className={`label label-${Utils.getLabelClassFromTaskState('TASK_SCHEDULED')}`}>SCHEDULED</span>;
      if (Utils.timestampWithinSeconds(cellData, config.pendingWithinSeconds)) {
        label = <span className={`label label-${Utils.getLabelClassFromTaskState('TASK_PENDING')}`}>PENDING</span>;
      } else if (cellData < Date.now() - config.pendingWithinSeconds * 1000) {
        label = <span className={`label label-${Utils.getLabelClassFromTaskState('TASK_OVERDUE')}`}>OVERDUE</span>;
      }
      return (
        <div>
          {Utils.timestampFromNow(cellData)} {label}
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
      (rowData) => rowData.taskId.deployId
    }
    cellRender={(deployId, task) => (
      <Link to={`request/${task.taskId.requestId}/deploy/${deployId}`}>{deployId}</Link>
    )}
    sortable={true}
  />
);

export const PendingDeployId = (
  <Column
    label="Deploy ID"
    id="pendingDeployId"
    key="pendingDeployId"
    cellData={
      (rowData) => rowData.pendingTask.pendingTaskId
    }
    sortData={(cellData) => cellData.deployId}
    cellRender={(cellData) =>
      <Link to={`request/${cellData.requestId}/deploy/${cellData.deployId}`}>{cellData.deployId}</Link>
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
    cellRender={(cellData) => (
      <div className="hidden-xs">
        <RunNowButton requestId={cellData.pendingTask.pendingTaskId.requestId} />
        <JSONButton className="inline" object={cellData} showOverlay={true}>
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

const logTooltip = (
  <ToolTip id="log">
    Logs
  </ToolTip>
);

export const LogLinkAndActions = (logPath, requestType) => (
  <Column
    label=""
    id="logLink"
    key="logLink"
    className="actions-column"
    cellData={(rowData) => rowData.taskId}
    cellRender={(taskId, rowData) => (
      <div className="hidden-xs">
        <KillTaskButton
          taskId={taskId.id}
          shouldShowWaitForReplacementTask={Utils.isIn(requestType, ['SERVICE', 'WORKER'])}
        />
        <OverlayTrigger placement="top" id="view-log-overlay" overlay={logTooltip}>
          <Link to={Utils.tailerPath(taskId.id, logPath)} title="Log">
            <Glyphicon glyph="file" />
          </Link>
        </OverlayTrigger>
        <JSONButton className="inline" object={rowData} showOverlay={true}>
          {'{ }'}
        </JSONButton>
      </div>
    )}
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
        <JSONButton className="inline" object={cellData} showOverlay={true}>
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
      (rowData) => rowData.instanceNo ? rowData.instanceNo : rowData.taskId.instanceNo
    }
    sortable={true}
  />
);

export const InstanceNumberWithHostname = (
  <Column
    label="Instance"
    id="instanceNo"
    key="instanceNo"
    cellData={
      (rowData) => rowData.instanceNo ? rowData.instanceNo : rowData.taskId.instanceNo
    }
    cellRender={
      (cellData, rowData) => (
        <Link to={`task/${rowData.taskId ? rowData.taskId.id : rowData.id}`}>
          {cellData} - {Utils.humanizeSlaveHostName(rowData.host ? rowData.host : rowData.taskId.host)}
        </Link>
      )
    }
    sortable={true}
  />
);

export const Health = (
  <Column
    label=""
    id="health"
    key="health"
    cellData={
      (rowData) => rowData.health
    }
    cellRender={
      (cellData) => {
        let glyph;
        let colorClass;
        if (cellData === "healthy" || cellData === "cleaning") {
          glyph = "ok";
          colorClass = "color-success";
        } else if (cellData === "pending") {
          glyph = "question-sign";
        } else {
          glyph = "hourglass";
          colorClass = "color-info"
        }
        const tooltip = (
          <ToolTip id="view-task-health">
            {cellData}
          </ToolTip>
        )
        return (
          <OverlayTrigger placement="top" id="view-task-health-overlay" overlay={tooltip}>
            <Glyphicon className={colorClass} glyph={glyph} />
          </OverlayTrigger>
        );
      }
    }
    sortable={true}
  />
);
