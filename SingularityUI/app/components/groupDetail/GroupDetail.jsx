import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { FetchGroups } from '../../actions/api/requestGroups';

import { Tabs, Tab } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';

const GroupDetail = ({group, location}) => {
  const tabs = group.requestIds.map((requestId, index) => {
    return (
      <Tab key={index} eventKey={index} title={requestId}>
        <div className="tab-container">
          <RequestDetailPage index={index} params={{requestId}} location={location} showBreadcrumbs={false} />
        </div>
      </Tab>
    );
  });

  return (
    <div>
      <h1>{group.id}</h1>
      <Tabs id="request-ids">
        {tabs}
      </Tabs>
    </div>
  );
};

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object
};

function mapStateToProps(state, ownProps) {
  const group = _.find(state.api.requestGroups.data, (g) => g.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group
  });
}

const mapDispatchToProps = (dispatch) => {
  return {
    fetchGroups: () => dispatch(FetchGroups.trigger())
  };
};

function refresh(props) {
  return props.fetchGroups();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(GroupDetail, (props) => `Group ${props.params.groupId}`, refresh, false));
