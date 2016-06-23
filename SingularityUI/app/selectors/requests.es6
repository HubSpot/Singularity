import { createSelector } from 'reselect';

import Utils from '../utils';

const getStarred = (state) => new Set(state.ui.starred);
const getRequests = (state) => state.api.requests;
const getUser = (state) => state.api.user;

export const combineStarredWithRequests = createSelector(
  [getStarred, getRequests],
  (starredData, requestsAPI) => {
    return requestsAPI.data.map((r) => {
      return {
        ...r,
        starred: starredData.has(r.request.id)
      };
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

      for (const owner of requestOwners) {
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
      total: userRequests.length,
      ON_DEMAND: 0,
      SCHEDULED: 0,
      WORKER: 0,
      RUN_ONCE: 0,
      SERVICE: 0
    };

    for (const r of userRequests) {
      userRequestTotals[r.request.requestType] += 1;
    }

    return userRequestTotals;
  }
);
