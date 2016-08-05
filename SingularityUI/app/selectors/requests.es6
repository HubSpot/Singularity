import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';

import Utils from '../utils';

const getRequestsAPI = (state) => state.api.requests;
const getUserAPI = (state) => state.api.user;
const getSearchFilter = (state) => state.ui.requestsPage;

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
    return requests.filter((request) => starredData.has(request.request.id));
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

    return requests.filter((request) => {
      const activeDeployUser = Utils.maybe(
        request,
        ['requestDeployState', 'activeDeploy', 'user']
      );

      if (activeDeployUser) {
        const activeDeployUserTrimmed = activeDeployUser.split('@')[0];
        if (deployUserTrimmed === activeDeployUserTrimmed) {
          return true;
        }
      }

      const requestOwners = request.owners;
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

    for (const request of userRequests) {
      userRequestTotals[request.request.requestType] += 1;
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
      filteredRequests = filteredRequests.filter((request) => {
        return searchFilter.typeFilter === request.request.requestType;
      });
    }

    // filter by state
    filteredRequests = filteredRequests.filter((request) => {
      return searchFilter.stateFilter.indexOf(request.state) > -1;
    });

    const getUser = (request) => {
      if ('requestDeployState' in request && 'activeDeploy' in request.requestDeployState) {
        return request.requestDeployState.activeDeploy.user || '';
      }
      return null;
    };

    // filter by text
    if (searchFilter.textFilter.length < 3) {
      // Don't start filtering by text until string has some length
      return filteredRequests;
    }
    if (Utils.isGlobFilter(searchFilter.textFilter)) {
      const byId = filteredRequests.filter((request) => {
        return micromatch.any(request.request.id, `${searchFilter.textFilter}*`);
      });
      const byUser = filteredRequests.filter((request) => {
        const user = getUser(request);
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
          extract: (request) => request.request.id
        }
      );

      const byUser = fuzzy.filter(
        searchFilter.textFilter,
        filteredRequests,
        {
          extract: (request) => getUser(request) || ''
        }
      );

      filteredRequests = _.uniq(
        _.pluck(
          _.sortBy(
            _.union(byUser, byId),
            (request) => {
              return Utils.fuzzyAdjustScore(searchFilter.textFilter, request);
            }
          ),
          'original'
        ).reverse()
      );
    }

    return filteredRequests;
  }
);
