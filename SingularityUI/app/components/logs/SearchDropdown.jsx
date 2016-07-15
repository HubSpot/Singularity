import React from 'react';
import ReactDOM from 'react-dom';
import classNames from 'classnames';

import { connect } from 'react-redux';
import { setCurrentSearch } from '../../actions/log';

class SearchDropdown extends React.Component {
  constructor(...args) {
    super(...args);
    this.state = {
      searchValue: this.props.search
    };
    _.bindAll(this, 'handleSearchToggle', 'handleSearchUpdate', 'toggleSearchDropdown', 'handleSearchKeyDown');
  }

  handleSearchToggle() {
    ReactDOM.findDOMNode(this.refs.searchInput).focus()
  }

  handleSearchUpdate() {
    this.props.setCurrentSearch(this.state.searchValue)
  }

  toggleSearchDropdown() {
    $(ReactDOM.findDOMNode(this.refs.searchButton)).dropdown("toggle")
  }

  handleSearchKeyDown(event) {
    if (event.keyCode == 13) { // Enter: commit search and close
      this.handleSearchUpdate();
      this.toggleSearchDropdown();
    } else if (event.keyCode == 27) { // Escape: clear search and commit
      this.setState({searchValue: this.props.search});
      this.toggleSearchDropdown();
    }
  }

  render() {
    let classes = ['btn', 'btn-sm', 'dropdown-toggle'];
    if (this.props.search === '') {
      classes.push('btn-default');
    } else {
      classes.push('btn-info');
    }
    return <div className="btn-group" title="Grep">
      <button ref="searchButton" id="searchDDToggle" type="button" className={classNames(classes)} data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" onClick={this.handleSearchToggle}>
        <span className="glyphicon glyphicon-search"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        <li>
          <div className="input-group log-search">
            <input ref="searchInput" type="text" className="form-control" placeholder="Grep Logs" value={this.state.searchValue} onKeyDown={this.handleSearchKeyDown} onChange={(e) => { this.setState({searchValue: e.target.value}); }} />
            <span className="input-group-btn">
              <button className="btn btn-info no-margin" type="button" onClick={this.handleSearchUpdate}><span className="glyphicon glyphicon-search"></span></button>
            </span>
          </div>
        </li>
      </ul>
    </div>
  }
};

SearchDropdown.propTypes = {
  search: React.PropTypes.string.isRequired
};

function mapStateToProps(state) {
  return {
    search: state.search
  };
};

const mapDispatchToProps = { setCurrentSearch };

module.exports = connect(mapStateToProps, mapDispatchToProps)(SearchDropdown);
