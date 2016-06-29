import React, { PropTypes } from 'react';
import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';

import UnpauseButton from './UnpauseButton';
import RemoveButton from './RemoveButton';
import RunNowButton from './RunNowButton';
import ScaleButton from './ScaleButton';

// use this only with combineStarredWithRequests selector
export const Starred = (changeStar, sortable) => {
  return <Column
    label=""
    id="starred"
    key="starred"
    cellData={
      (rowData) => 'starred' in rowData
    }
    cellRender={
      (cellData, rowData) => {
        return (
          <a className="star" data-starred={cellData} onClick={
            () => changeStar(rowData.request.id)
          }>
            <span className="glyphicon glyphicon-star"></span>
          </a>
        );
      }
    }
    sortable={sortable}
  />
};

Starred.propTypes = {
  changeStar: PropTypes.func,
  sortable: PropTypes.bool
};

export const DeployUser = (
  <Column
    label="Deploy User"
    id="user"
    key="user"
    cellData={
      (rowData) => {
        const activeDeployUser = Utils.maybe(rowData, [
          'requestDeployState',
          'activeDeploy',
          'user'
        ]);

        if (activeDeployUser !== undefined) {
          // assume user is an email address
          return activeDeployUser.split('@')[0];
        }

        return '';
      }
    }
    sortable={true}
  />
);

export const LastDeploy = (
  <Column
    label="Time of Last Deploy"
    id="lastDeploy"
    key="lastDeploy"
    cellData={
      (rowData) => {
        const activeDeployTimestamp = Utils.maybe(rowData, [
          'requestDeployState',
          'activeDeploy',
          'timestamp'
        ], null);

        return activeDeployTimestamp;
      }
    }
    sortData={(cellData, rowData) => cellData || 0}
    cellRender={
      (cellData) => {
        if (cellData) {
          return Utils.timeStampFromNow(cellData);
        }
        return '';
      }
    }
    sortable={true}
  />
);

export const RequestId = (
  <Column
    label="Request"
    id="requestId"
    key="requestId"
    className="keep-in-check"
    cellData={
      (rowData) => rowData.request.id
    }
    sortData={(cellData) => cellData.toLowerCase()}
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/request/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable={true}
  />
);

export const State = (
  <Column
    label="Status"
    id="state"
    key="state"
    cellData={
      (rowData) => Utils.humanizeText(rowData.state)
    }
    sortable={true}
  />
);

export const Type = (
  <Column
    label="Type"
    id="type"
    key="type"
    cellData={
      (rowData) => Utils.humanizeText(rowData.request.requestType)
    }
    sortable={true}
  />
);

export const Instances = (
  <Column
    label="Instances"
    id="instances"
    key="instances"
    cellData={
      (rowData) => rowData.request.instances
    }
    sortData={(cellData) => cellData || 0}
    sortable={true}
  />
);

export const DeployId = (
  <Column
    label="Deploy ID"
    id="deployId"
    key="deployId"
    cellData={
      (rowData) => rowData.requestDeployState && rowData.requestDeployState.activeDeploy
    }
    sortData={(cellData, rowData) => cellData ? cellData.deployId : ''}
    cellRender={(cellData) => {
        if (cellData) {
          return (
            <a href={`${config.appRoot}/request/${cellData.requestId}/deploy/${cellData.deployId}`}>{cellData.deployId}</a>
          );
        }
      }
    }
    sortable={true}
  />
);

export const Schedule = (
  <Column
    label="Schedule"
    id="schedule"
    key="schedule"
    cellData={
      (rowData) => rowData.request.quartzSchedule
    }
    sortData={(cellData) => cellData || ''}
    sortable={true}
  />
);

export const Actions = (removeAction, unpauseAction, runAction, fetchRun, fetchRunHistory, fetchTaskFiles, scaleAction) => {
  return <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellData={
      (rowData) => rowData
    }
    cellRender={
      (rowData) => {
        const edit = !config.hideNewRequestButton && (
          <a href={`${config.appRoot}/requests/edit/${rowData.request.id}`} alt="Edit">
            <span className="glyphicon glyphicon-edit"></span>
          </a>
        );

        const unpause = rowData.state === 'PAUSED' && (
          <UnpauseButton requestId={rowData.request.id} unpauseAction={unpauseAction} />
        );

        const scale = rowData.canBeScaled && (
          <ScaleButton requestId={rowData.request.id} scaleAction={scaleAction} currentInstances={rowData.request.instances} />
        );

        const runNow = rowData.canBeRunNow && (
          <RunNowButton
            requestId={rowData.request.id}
            runAction={runAction}
            fetchRunAction={fetchRun}
            fetchRunHistoryAction={fetchRunHistory}
            fetchTaskFilesAction={fetchTaskFiles}
          />
        );

        return (
          <div className="hidden-xs">
            {scale}
            {runNow}
            {unpause}
            <RemoveButton requestId={rowData.request.id} removeAction={removeAction} />
            <JSONButton className="inline" object={rowData}>
              {'{ }'}
            </JSONButton>
            {edit}
          </div>
        );
      }
    }
  />;
};
