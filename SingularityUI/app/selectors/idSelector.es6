import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../utils';

const getOptions = (state) => state.options;
const getFilter = (state) => state.filter;

export default createSelector([getOptions, getFilter], (options, filter) => {
  let filteredOptions = options;
  const getId = (parent) => parent.id || '';
  if (Utils.isGlobFilter(filter.searchFilter)) {
    const idMatches = _.filter(filteredOptions, (parent) => (
      micromatch.isMatch(getId(parent), `${filter.searchFilter}*`)
    ));
    filteredOptions = idMatches;
  } else if (filter.searchFilter) {
    // Allow searching by the first letter of each word by applying same
    // search heuristics to just the upper case characters of each option
    const idMatches = fuzzy.filter(filter.searchFilter, filteredOptions, {
      extract: Utils.isAllUpperCase(filter.searchFilter)
        ? (parent) => Utils.getUpperCaseCharacters(getId(parent))
        : getId,
    });
    filteredOptions = Utils.fuzzyFilter(filter.searchFilter, idMatches);
  }
  return filteredOptions;
});
