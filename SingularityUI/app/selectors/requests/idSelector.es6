import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../../utils';

const getRequests = (state) => state.requests;
const getFilter = (state) => state.filter;

export default createSelector([getRequests, getFilter], (requests, filter) => {
  console.log(filter)
  let filteredRequests = requests;
  const getId = (requestParent) => requestParent.id || '';
  if (Utils.isGlobFilter(filter.searchFilter)) {
    const idMatches = _.filter(filteredRequests, (requestParent) => (
      micromatch.isMatch(getId(requestParent), `${filter.searchFilter}*`)
    ));
    filteredRequests = idMatches;
  } else {
    // Allow searching by the first letter of each word by applying same
    // search heuristics to just the upper case characters of each option
    const idMatches = fuzzy.filter(filter.searchFilter, filteredRequests, {
      extract: Utils.isAllUpperCase(filter.searchFilter)
        ? (requestParent) => Utils.getUpperCaseCharacters(getId(requestParent))
        : getId,
    });
    filteredRequests = Utils.fuzzyFilter(filter.searchFilter, idMatches);
  }
  return filteredRequests;
});
