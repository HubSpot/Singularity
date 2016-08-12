import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import _ from 'underscore';

import Utils from '../utils';

const getRequestsAPI = (state) => state.api.requests;
const getUserAPI = (state) => state.api.user;
const getSearchFilter = (state) => state.ui.requestsPage;

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

export const getFilteredRequests = createSelector(
  [getSearchFilter, getRequestsAPI],
  (searchFilter, requestsAPI) => {
    let filteredRequests = requestsAPI.data;

    // filter by type
    if (searchFilter.typeFilter !== 'ALL') {
      filteredRequests = filteredRequests.filter((r) => {
        return searchFilter.typeFilter === r.request.requestType;
      });
    }

    // filter by state
    filteredRequests = filteredRequests.filter((r) => {
      return searchFilter.stateFilter.indexOf(r.state) > -1;
    });

    const getUser = (r) => {
      if ('requestDeployState' in r && 'activeDeploy' in r.requestDeployState) {
        return r.requestDeployState.activeDeploy.user || '';
      }
      return null;
    };

    // filter by text
    if (searchFilter.textFilter.length < 3) {
      // Don't start filtering by text until string has some length
      return filteredRequests;
    }
    if (Utils.isGlobFilter(searchFilter.textFilter)) {
      const byId = filteredRequests.filter((r) => {
        return micromatch.any(r.request.id, `${searchFilter.textFilter}*`);
      });
      const byUser = filteredRequests.filter((r) => {
        const user = getUser(r);
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
          extract: (r) => r.request.id
        }
      );

      const byUser = fuzzy.filter(
        searchFilter.textFilter,
        filteredRequests,
        {
          extract: (r) => getUser(r) || ''
        }
      );

      filteredRequests = _.uniq(
        _.pluck(
          _.sortBy(
            _.union(byUser, byId),
            (r) => {
              return Utils.fuzzyAdjustScore(searchFilter.textFilter, r);
            }
          ),
          'original'
        ).reverse()
      );
    }

    return filteredRequests;
  }
);
