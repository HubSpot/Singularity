import React, { PropTypes } from 'react';
import Column from '../common/table/Column';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';

import UnpauseButton from './UnpauseButton';
import RemoveButton from './RemoveButton';

// use this only with combineStarredWithRequests selector
export const Starred = ({changeStar, sortable}) => (
  <Column
    label=""
    id="starred"
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
);

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

export const Actions = (removeAction) => {
  return <Column
    label=""
    id="actions"
    key="actions"
    className="actions-column"
    cellData={
      (rowData) => rowData.request.id
    }
    cellRender={
      (requestId, rowData) => {
        const edit = !config.hideNewRequestButton && (
          <a href={`${config.appRoot}/requests/edit/${requestId}`} alt="Edit">
            <span className="glyphicon glyphicon-edit"></span>
          </a>
        );

        return (
          <div className="hidden-xs">
            <RemoveButton requestId={requestId} removeAction={removeAction} />
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
