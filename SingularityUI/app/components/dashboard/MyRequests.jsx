import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import * as RequestsSelectors from '../../selectors/requests';

import RequestCounts from './RequestCounts';
import RequestCount from './RequestCount';

const MyRequests = ({userRequestTotals, userAPI}) => {
  const totals = userRequestTotals;
  const deployUser = Utils.maybe(userAPI.data, [
    'user',
    'id'
  ]);

  return (
    <RequestCounts>
      <RequestCount
        label={'total'}
        count={totals.total}
        link={`requests/all/active/all/${deployUser}`}
      />
      <RequestCount
        label={'on demand'}
        count={totals.ON_DEMAND}
        link={`requests/all/active/ON_DEMAND/${deployUser}`}
      />
      <RequestCount
        label={'worker'}
        count={totals.WORKER}
        link={`requests/all/active/WORKER/${deployUser}`}
      />
      <RequestCount
        label={'scheduled'}
        count={totals.SCHEDULED}
        link={`requests/all/active/SCHEDULED/${deployUser}`}
      />
      <RequestCount
        label={'run once'}
        count={totals.RUN_ONCE}
        link={`requests/all/active/RUN_ONCE/${deployUser}`}
      />
      <RequestCount
        label={'service'}
        count={totals.SERVICE}
        link={`requests/all/active/SERVICE/${deployUser}`}
      />
    </RequestCounts>
  );
};

MyRequests.propTypes = {
  userRequestTotals: PropTypes.object.isRequired,
  userAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state) => {
  return {
    userRequestTotals: RequestsSelectors.getUserRequestTotals(state),
    userAPI: state.api.user
  };
};

export default connect(
  mapStateToProps
)(MyRequests);
