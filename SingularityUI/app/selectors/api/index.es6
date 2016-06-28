import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import _ from 'underscore';

import Utils from '../../utils';

const getSearchFilter = (state) => state.ui.requestsPage;
const getRequests = (state) => state.api.requests;

export const getFilteredRequests = createSelector(
  [ getSearchFilter, getRequests ],
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
    }

    // filter by text
    if (searchFilter.textFilter.length < 3) {
      // Don't start filtering by text until string has some length
      return filteredRequests;
    }
    if (Utils.isGlobFilter(searchFilter.textFilter)) {
      const byId = filteredRequests.filter((r) => {
        return micromatch.any(r.request.id, searchFilter.textFilter + '*');
      });
      const byUser = filteredRequests.filter((r) => {
        const user = getUser(r);
        if (user !== null) {
          return micromatch.any(user, searchFilter.textFilter + '*');
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
