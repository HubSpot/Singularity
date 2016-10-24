import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import Utils from '../utils';

const getRequestsAPI = (state) => state.api.requests;
const getUserAPI = (state) => state.api.user;
const getRequests = (state) => state.requestsInState;
const getFilter = (state) => state.filter;

function findRequestIds(requests) {
  return _.map(requests, (request) => {
    return _.extend({}, request, {id: request.request ? request.request.id : request.requestId});
  });
}

export const getStarred = (state) => new Set(state.ui.starred);

export const getStarredRequests = createSelector(
  [getStarred, getRequestsAPI],
  (starredData, requestsAPI) => {
    const requests = findRequestIds(requestsAPI.data);
    return requests.filter((requestParent) => starredData.has(requestParent.request.id));
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

    return requests.filter((requestParent) => {
      const activeDeployUser = Utils.maybe(
        requestParent,
        ['requestDeployState', 'activeDeploy', 'user']
      );

      if (activeDeployUser) {
        const activeDeployUserTrimmed = activeDeployUser.split('@')[0];
        if (deployUserTrimmed === activeDeployUserTrimmed) {
          return true;
        }
      }

      const requestOwners = requestParent.request.owners;
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

    for (const requestParent of userRequests) {
      userRequestTotals[requestParent.request.requestType] += 1;
    }

    return userRequestTotals;
  }
);

export default createSelector([getRequests, getFilter], (requests, filter) => {
  let filteredRequests = requests;

  // Filter by state
  let stateFilter = null;
  switch (filter.state) {
    case 'activeDeploy':
      stateFilter = (requestParent) => requestParent.hasActiveDeploy;
      break;
    case 'noDeploy':
      stateFilter = (requestParent) => !requestParent.hasActiveDeploy;
      break;
    default:
      break;
  }
  if (stateFilter) {
    filteredRequests = _.filter(filteredRequests, stateFilter);
  }

  // Filter by request type
  if (!_.contains(['pending', 'cleanup'], filter.type)) {
    filteredRequests = _.filter(filteredRequests, (requestParent) => requestParent.request && _.contains(filter.subFilter, requestParent.request.requestType));
  }

  // Filter by glob or string match
  if (filter.searchFilter) {
    const id = (requestParent) => requestParent.id || '';
    const user = (requestParent) => `${requestParent.hasActiveDeploy ? requestParent.requestDeployState.activeDeploy.user : ''}`;

    if (Utils.isGlobFilter(filter.searchFilter)) {
      const res1 = _.filter(filteredRequests, (requestParent) => {
        return micromatch.any(user(requestParent).toLowerCase(), `*${filter.searchFilter.toLowerCase()}*`);
      });
      const res2 = _.filter(filteredRequests, (requestParent) => {
        return micromatch.any(id(requestParent).toLowerCase(), `*${filter.searchFilter.toLowerCase()}*`);
      });
      filteredRequests = _.sortBy(_.union(res1, res2), (requestParent) => (micromatch.any(id(requestParent).toLowerCase(), `${filter.searchFilter.toLowerCase()}*`) ? 1 : 0)).reverse();
    } else {
      const res1 = _.filter(filteredRequests, requestParent => id(requestParent).toLowerCase().indexOf(filter.searchFilter.toLowerCase()) > -1);
      const res2 = _.filter(filteredRequests, requestParent => user(requestParent).toLowerCase().indexOf(filter.searchFilter.toLowerCase()) > -1);
      filteredRequests = _.uniq(_.sortBy(_.union(res1, res2), (requestParent) => (id(requestParent).toLowerCase().startsWith(filter.searchFilter.toLowerCase()) ? 1 : 0)).reverse());
    }
  }

  return filteredRequests;
});
