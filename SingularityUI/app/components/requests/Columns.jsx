import React from 'react';
import Column from '../common/table/Column';
import moment from 'moment';

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
        // optional hell
        if ('requestDeployState' in rowData) {
          const requestDeployState = rowData.requestDeployState;
          if ('activeDeploy' in requestDeployState) {
            const activeDeploy = requestDeployState.activeDeploy;
            if ('user' in activeDeploy) {
              const user = activeDeploy.user;
              // assume user is an email address
              return user.split('@')[0];
            }
          }
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
        if ('requestDeployState' in rowData) {
          const requestDeployState = rowData.requestDeployState;
          if ('activeDeploy' in requestDeployState) {
            const activeDeploy = requestDeployState.activeDeploy;
            return activeDeploy.timestamp;
          }
        }
        return null;
      }
    }
    cellRender={
      (cellData) => {
        if (cellData !== null) {
          const fromNow = moment(cellData).fromNow();
          const when = moment(cellData).calendar();
          return `${fromNow} (${when})`;
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
