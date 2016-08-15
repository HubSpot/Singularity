import { createSelector } from 'reselect';
import _ from 'underscore';

import Utils from '../utils';

const getRequestsAPI = (state) => state.api.requests;
const getUserAPI = (state) => state.api.user;

function findRequestIds(requests) {
  return _.map(requests, (r) => {
    return _.extend({}, r, {id: r.request ? r.request.id : r.requestId});
  });
}

export const getStarred = (state) => new Set(state.ui.starred);

export const getStarredRequests = createSelector(
  [getStarred, getRequestsAPI],
  (starredData, requestsAPI) => {
    const requests = findRequestIds(requestsAPI.data);
    return requests.filter((r) => starredData.has(r.request.id));
  }
);

export const getUserRequests = createSelector(
  [getUserAPI, getRequestsAPI],
  (userAPI, requestsAPI) => {
    const deployUserTrimmed = Utils.maybe(
      userAPI.data,
      ['user', 'email'],
      ''
    ).split('@')[0];

    const requests = findRequestIds(requestsAPI.data);

    return requests.filter((r) => {
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

      const requestOwners = r.request.owners;
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
