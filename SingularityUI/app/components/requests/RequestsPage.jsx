import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as RequestsActions from '../../actions/api/requests';
import * as RequestsPageActions from '../../actions/ui/requestsPage';

import Sidebar from '../common/Sidebar';
import MainContent from '../common/MainContent';
import TabBar from '../common/TabBar';
import TabBarFilterOption from '../common/tabBar/TabBarFilterOption';
import SearchBar from '../common/SearchBar';

import FilterOptionState from '../../containers/requests/FilterOptionState';
import RequestsTable from './RequestsTable';

class RequestsPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsPage';
  }

  render() {
    const { requests, actions } = this.props;
    return (
      <div className='requests-page'>
        <Sidebar>
          <FilterOptionState
            label={'Active'}
            filterValue={'ACTIVE'}
          />
          <FilterOptionState
            label={'Cooling down'}
            filterValue={'SYSTEM_COOLDOWN'}
          />
          <FilterOptionState
            label={'Paused'}
            filterValue={'PAUSED'}
          />
        </Sidebar>
        <MainContent>
          <TabBar>
            <TabBarFilterOption
              isEnabled={true}
              filterName={'All types'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'On-demand'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Worker'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Scheduled'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Run-once'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Service'}
              numberOfItems={0}
              onChange={console.log}
            />
          </TabBar>
          <SearchBar />
          <RequestsTable />
        </MainContent>
      </div>
    );
  }
}

RequestsPage.propTypes = {
  requests: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

const searchRequests = (requests, search) => {
  return requests.filter((r) => {
    return search.state.indexOf(r.state) >= -1;
  });
}


const mapStateToProps = (state) => {
  return {
    requests: state.requests
  }
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(RequestsActions, dispatch)
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestsPage);
