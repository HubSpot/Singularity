import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import _ from 'underscore';

import Utils from '../utils';

const getRequestsAPI = (state) => state.api.requests;
const getUserAPI = (state) => state.api.user;
const getSearchFilter = (state) => state.ui.requestsPage;
const getCurrentGroup = (state) => state.ui.dashboard.currentGroup;

function findRequestIds(requests) {
  return _.map(requests, (request) => {
    return _.extend({}, request, {id: request.request ? request.request.id : request.requestId});
  });
}

export const getStarredRequests = createSelector(
  [getUserAPI, getRequestsAPI],
  (userAPI, requestsAPI) => {
    const starredRequests = Utils.maybe(userAPI, ['data', 'settings', 'starredRequestIds'], []);
    const requests = findRequestIds(requestsAPI.data);
    return requests.filter((requestParent) => _.contains(starredRequests, requestParent.request.id));
  }
);

export const getUserGroupRequests = createSelector(
  [getCurrentGroup, getRequestsAPI],
  (currentGroup, requestsAPI) => {
    const requests = findRequestIds(requestsAPI.data);
    return requests.filter((requestParent) => requestParent.request.group === currentGroup);
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

export const getFilteredRequests = createSelector(
  [getSearchFilter, getRequestsAPI],
  (searchFilter, requestsAPI) => {
    let filteredRequests = requestsAPI.data;

    // filter by type
    if (searchFilter.typeFilter !== 'ALL') {
      filteredRequests = filteredRequests.filter((requestParent) => {
        return searchFilter.typeFilter === requestParent.request.requestType;
      });
    }

    // filter by state
    filteredRequests = filteredRequests.filter((requestParent) => {
      return searchFilter.stateFilter.indexOf(requestParent.state) > -1;
    });

    const getUser = (requestParent) => {
      if ('requestDeployState' in requestParent && 'activeDeploy' in requestParent.requestDeployState) {
        return requestParent.requestDeployState.activeDeploy.user || '';
      }
      return null;
    };

    // filter by text
    if (searchFilter.textFilter.length < 3) {
      // Don't start filtering by text until string has some length
      return filteredRequests;
    }
    if (Utils.isGlobFilter(searchFilter.textFilter)) {
      const byId = filteredRequests.filter((requestParent) => {
        return micromatch.any(requestParent.request.id, `${searchFilter.textFilter}*`);
      });
      const byUser = filteredRequests.filter((requestParent) => {
        const user = getUser(requestParent);
        if (user !== null) {
          return micromatch.any(user, `${searchFilter.textFilter}*`);
        }
        return false;
      });
      filteredRequests = _.uniq(_.union(byUser, byId)).reverse();
    } else {
      // somewhere, in the history of Request searching, this was labeled a hack
      // time has passed
      // the comment was lost to refactors
      // this is no longer considered a hack
      // todo: remove hack
      const byId = fuzzy.filter(
        searchFilter.textFilter,
        filteredRequests,
        {
          extract: (requestParent) => requestParent.request.id
        }
      );

      const byUser = fuzzy.filter(
        searchFilter.textFilter,
        filteredRequests,
        {
          extract: (requestParent) => getUser(requestParent) || ''
        }
      );

      filteredRequests = Utils.fuzzyFilter(searchFilter.textFilter, _.union(byUser, byId));
    }

    return filteredRequests;
  }
);
