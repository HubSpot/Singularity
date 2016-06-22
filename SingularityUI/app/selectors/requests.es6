import { createSelector } from 'reselect';

import Utils from '../utils';

const getStarred = (state) => state.ui.starred;
const getRequests = (state) => state.api.requests;
const getUser = (state) => state.api.user;

export const combineStarredWithRequests = createSelector(
  [getStarred, getRequests],
  (starredData, requestsAPI) => {
    // TODO: make this use two sorted lists
    // to combine the data
    // because this is O(awful)
    return requestsAPI.data.map((r) => {
      if (starredData.indexOf(r.request.id) > -1) {
        return {
          ...r,
          starred: true
        };
      }
      return r;
    });
  }
);

export const getUserRequests = createSelector(
  [getUser, getRequests],
  (userAPI, requestsAPI) => {
    const deployUserTrimmed = Utils.maybe(
      userAPI.data,
      ['user', 'email'],
      ''
    ).split('@')[0];

    return requestsAPI.data.filter((r) => {
      const activeDeployUser = Utils.maybe(
        r,
        ['requestDeployState', 'activeDeploy', 'user']
      );

      if (activeDeployUser) {
        const activeDeployUserTrimmed = activeDeployUser.split('@')[0];
        if (deployUserTrimmed === activeDeployUserTrimmed) {
          return true;
        }
      }

      const requestOwners = r.owners;
      if (requestOwners === undefined) {
        return false;
      }

      for (let owner of requestOwners) {
        if (deployUserTrimmed === owner.split('@')[0]) {
          return true;
        }
      }

      return false;
    });
  }
);


export const getUserRequestTotals = createSelector(
  [getUserRequests],
  (userRequests) => {
    const userRequestTotals = {
      all: userRequests.length,
      onDemand: 0,
      worker: 0,
      scheduled: 0,
      runOnce: 0,
      service: 0
    };

    for (let r of userRequests) {
      const type = r.request.requestType;
      switch (type) {
        case 'ON_DEMAND':
          userRequestTotals.onDemand += 1;
          break;
        case 'SCHEDULED':
          userRequestTotals.scheduled += 1;
          break;
        case 'WORKER':
          userRequestTotals.worker += 1;
          break;
        case 'RUN_ONCE':
          userRequestTotals.runOnce += 1;
          break;
        case 'SERVICE':
          userRequestTotals.service += 1;
          break;
      }
    }

    return userRequestTotals;
  }
);
