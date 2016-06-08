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
import FilterOptionType from '../../containers/requests/FilterOptionType';
import RequestsTable from './RequestsTable';
import RequestsTableRow from './RequestsTableRow';

class RequestsPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsPage';
  }

  render() {
    const { requests } = this.props;

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
            <FilterOptionType
              label={'All types'}
              filterValue={'ALL'}
            />
            <FilterOptionType
              label={'On-demand'}
              filterValue={'ON_DEMAND'}
            />
            <FilterOptionType
              label={'Worker'}
              filterValue={'WORKER'}
            />
            <FilterOptionType
              label={'Scheduled'}
              filterValue={'SCHEDULED'}
            />
            <FilterOptionType
              label={'Run-once'}
              filterValue={'RUN_ONCE'}
            />
            <FilterOptionType
              label={'Service'}
              filterValue={'SERVICE'}
            />
          </TabBar>
          <SearchBar />
          <RequestsTable>
            {requests.data.slice(0, 10).map((r) => <RequestsTableRow requestParent={r} />)}
          </RequestsTable>
        </MainContent>
      </div>
    );
  }
}

RequestsPage.propTypes = {
  requests: PropTypes.object.isRequired
};

const mapStateToProps = (state) => {
  return {
    requests: state.requests
  }
};

const mapDispatchToProps = (dispatch) => {
  return {
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestsPage);
