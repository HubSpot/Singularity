import React from 'react';
import Column from '../common/table/Column';
import moment from 'moment';

import Utils from '../../utils';

// use this only with combineStarredWithRequests selector
export const Starred = (changeStar) => (
  <Column
    label=''
    id='starred'
    cellData={
      (rowData) => 'starred' in rowData
    }
    sortable
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
    sortable
  />
);

export const State = (
  <Column
    label='Status'
    id='state'
    cellData={
      (rowData) => rowData.state
    }
    sortable
  />
);

export const Type = (
  <Column
    label='Type'
    id='type'
    cellData={
      (rowData) => rowData.request.requestType
    }
    sortable
  />
);
