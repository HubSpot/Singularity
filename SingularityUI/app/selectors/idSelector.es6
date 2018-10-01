import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../utils';

const getRequests = (state) => state.requests;
const getFilter = (state) => state.filter;

export default createSelector([getRequests, getFilter], (requests, filter) => {
  let filteredRequests = requests;
  const getId = (parent) => parent.id || '';
  if (Utils.isGlobFilter(filter.searchFilter)) {
    const idMatches = _.filter(filteredRequests, (parent) => (
      micromatch.isMatch(getId(parent), `${filter.searchFilter}*`)
    ));
    filteredRequests = idMatches;
  } else {
    // Allow searching by the first letter of each word by applying same
    // search heuristics to just the upper case characters of each option
    const idMatches = fuzzy.filter(filter.searchFilter, filteredRequests, {
      extract: Utils.isAllUpperCase(filter.searchFilter)
        ? (parent) => Utils.getUpperCaseCharacters(getId(parent))
        : getId,
    });
    filteredRequests = Utils.fuzzyFilter(filter.searchFilter, idMatches);
  }
  return filteredRequests;
});
