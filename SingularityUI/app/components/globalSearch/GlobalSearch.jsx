import React from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { withRouter } from 'react-router';
import { FetchRequestIds } from '../../actions/api/requests';
import { SetVisibility } from '../../actions/ui/globalSearch';
import { refresh } from '../../actions/ui/requestDetail';
import { push } from 'react-router-redux';
import { Link } from 'react-router';

import { Typeahead } from 'react-typeahead';
import key from 'keymaster';
import idSelector from '../../selectors/idSelector';

class GlobalSearch extends React.Component {

  static propTypes = {
    requests: React.PropTypes.array,
    visible: React.PropTypes.bool,
    getRequests: React.PropTypes.func,
    setVisibility: React.PropTypes.func,
    router: React.PropTypes.object,
  }

  constructor() {
    super();
    _.bindAll(this, 'optionSelected', 'getValueFromOption');
  }

  componentWillMount() {
    this.props.getRequests();

    // Key events with the 'input' scope get triggered even when an input is focused
    key.filter = (event) => {
      const tagName = (event.target || event.srcElement).tagName;
      key.setScope(/^(INPUT|TEXTAREA|SELECT)$/.test(tagName) ? 'input' : 'noInput');
      return true;
    };
    key('t, s', 'noInput', () => {
      this.props.setVisibility(true);
      return false;
    });
    key('esc, escape', 'input', () => this.props.setVisibility(false));
  }

  componentDidUpdate(prevProps) {
    if (this.props.visible && !prevProps.visible) {
      this.focus();
    }
  }

  componentWillUnmount() {
    key.unbind('t', 'noInput');
    key.unbind('s', 'noInput');
    key.unbind('esc, escape', 'input');
  }

  resetSelection() {
    return this.refs.typeahead.setState({
      selectionIndex: 0
    });
  }

  clear() {
    this.refs.typeahead.setEntryText('');
    this.resetSelection();
  }

  focus() {
    this.refs.typeahead.focus();
    this.resetSelection();
  }

  searchOptions(inputValue, options) {
    const searched = idSelector({
      options: options,
      filter: {
        searchFilter: inputValue
      }
    });

    return searched;
  }

  getValueFromOption(option) {
    return option.id;
  }

  optionSelected(requestIdObject) {
    const requestId = this.getValueFromOption(requestIdObject);
    this.props.push(`/request/${ requestId }`, { trigger: true });
    this.props.refresh(requestId);
    this.clear();
    this.props.setVisibility(false);
  }

  renderOption(option, index) {
    return (
      <Link to={`/request/${option.id}`} key={index}>
        {option.id}
      </Link>
    );
  }

  render() {
    const options = _.map(this.props.requests, (id) => ({
      id: id,
    }));

    const globalSearchClasses = classNames('global-search', {
      'global-search-active': this.props.visible
    });

    if (this.props.visible) {
      return (
        <div className={globalSearchClasses}>
          <div className="container">
            <div className="close-button-container">
              <a onClick={() => this.props.setVisibility(false)}>&times;</a>
            </div>

            <p className="hidden-xs text-muted tip">
              Protip: You can press <kbd>s</kbd> or <kbd>t</kbd> to reach this search page and <kbd>esc</kbd> to close it.
            </p>
            <Typeahead
              ref="typeahead"
              options={options}
              maxVisible={10}
              customClasses={{
                input: 'big-search-box'
              }}
              placeholder="Search all requests"
              onOptionSelected={this.optionSelected}
              displayOption={this.renderOption}
              searchOptions={this.searchOptions}
              formInputOption={this.getValueFromOption}
              inputDisplayOption={this.getValueFromOption}
            />
          </div>
        </div>
      );
    }
    return null;
  }
}

export default connect((state) => ({
  requests: state.api.requestIds.data,
  visible: state.ui.globalSearch.visible
}), {
  getRequests: FetchRequestIds.trigger,
  setVisibility: SetVisibility,
  push,
  refresh
})(withRouter(GlobalSearch));
