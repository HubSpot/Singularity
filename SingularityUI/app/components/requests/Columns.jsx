import React from 'react';
import Column from '../common/table/Column';
import moment from 'moment';

import Utils from '../../utils';

import JSONButton from '../common/JSONButton';

import UnpauseButton from './UnpauseButton';
import RemoveButton from './RemoveButton';

// use this only with combineStarredWithRequests selector
export const Starred = ({changeStar, sortable}) => (
  <Column
    label=''
    id='starred'
    cellData={
      (rowData) => 'starred' in rowData
    }
    cellRender={
      (cellData, rowData) => {
        return (
          <a className='star' data-starred={cellData} onClick={
            () => changeStar(rowData.request.id)
          }>
            <span className='glyphicon glyphicon-star'></span>
          </a>
        );
      }
    }
    sortable={sortable}
  />
);

export const DeployUser = (
  <Column
    label='Deploy User'
    id='user'
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
    sortable
  />
);

export const LastDeploy = (
  <Column
    label='Time of Last Deploy'
    id='lastDeploy'
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
    cellRender={
      (cellData) => {
        if (cellData !== null) {
          return Utils.timeStampFromNow(cellData);
        }
        return '';
      }
    }
    sortable
  />
);

export const RequestId = (
  <Column
    label='Request'
    id='requestId'
    cellData={
      (rowData) => rowData.request.id
    }
    cellRender={
      (cellData) => (
        <a href={`${config.appRoot}/request/${cellData}`}>
          {cellData}
        </a>
      )
    }
    sortable
  />
);

export const State = (
  <Column
    label='Status'
    id='state'
    cellData={
      (rowData) => Utils.humanizeText(rowData.state)
    }
    sortable
  />
);

export const Type = (
  <Column
    label='Type'
    id='type'
    cellData={
      (rowData) => Utils.humanizeText(rowData.request.requestType)
    }
    sortable
  />
);

export const Actions = (unpauseAction, removeAction, showEditButton = false) => (
  <Column
    label=''
    id='actions'
    className='actions-column'
    cellData={
      (rowData) => rowData.request.id
    }
    cellRender={
      (requestId, rowData) => {
        let maybeEditButton;
        if (showEditButton) {
          maybeEditButton = (
            <a href={`${config.appRoot}/requests/edit/${requestId}`} alt='Edit'>
              <span className='glyphicon glyphicon-edit'></span>
            </a>
          );
        }

        return (
          <div className='hidden-xs'>
            <UnpauseButton requestId={requestId} unpauseAction={unpauseAction} />
            <RemoveButton requestId={requestId} removeAction={removeAction} />
            <JSONButton className='inline' object={rowData}>
              {'{ }'}
            </JSONButton>
            {maybeEditButton}
          </div>
        );
      }
    }
  />
);
