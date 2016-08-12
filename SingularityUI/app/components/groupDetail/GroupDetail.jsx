import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { FetchGroups } from '../../actions/api/requestGroups';

import { Row, Col, Tabs, Tab, DropdownButton, MenuItem } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';
import MetadataButton from '../common/MetadataButton';
import PauseButton from '../requests/PauseButton';
import UnpauseButton from '../requests/UnpauseButton';
import BounceButton from '../requests/BounceButton';

class GroupDetail extends React.Component {

  // shouldComponentUpdate(nextProps, nextState) {
  //   console.log(nextState, this.state);
  //   return !_.isEqual(nextProps.requests, this.props.requests);
  // }

  render() {
    const {group, requests, location} = this.props;

    const tabs = group.requestIds.map((requestId, index) => {
      return (
        <Tab key={index} eventKey={index} title={requestId}>
          <div className="tab-container">
            <RequestDetailPage index={index} params={{requestId}} location={location} showBreadcrumbs={false} />
          </div>
        </Tab>
      );
    });
    const metadata = !_.isEmpty(group.metadata) && (
      <MetadataButton title={group.id} metadata={group.metadata}>View Metadata</MetadataButton>
    );
    const dropdown = (
      <DropdownButton bsStyle="primary" title="Apply to all" id="action-dropdown">
        <PauseButton requestId={_.first(group.requestIds)} isScheduled={false}>
          <MenuItem eventKey="1">Pause</MenuItem>
        </PauseButton>
        <UnpauseButton requestId={_.first(group.requestIds)} isScheduled={false}>
          <MenuItem eventKey="2">Unpause</MenuItem>
        </UnpauseButton>
        <BounceButton requestId={_.first(group.requestIds)}>
          <MenuItem eventKey="3">Bounce</MenuItem>
        </BounceButton>
      </DropdownButton>
    );
    console.log(requests);
    return (
      <div>
        <Row className="detail-header">
          <Col md={7} lg={6}>
            <h1>{group.id}</h1>
          </Col>
          <Col md={5} lg={6} className="button-container">
            {dropdown}
            {metadata}
          </Col>
        </Row>
        <Tabs id="request-ids">
          {tabs}
        </Tabs>
      </div>
    );
  }
}

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  requests: PropTypes.array
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  const requests = group && _.pick({...state.api.request}, (value, key) => _.contains(group.requestIds, key));
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    // requests,
    group
  });
};

const mapDispatchToProps = (dispatch) => {
  return {
    fetchGroups: () => dispatch(FetchGroups.trigger())
  };
};

function refresh(props) {
  return props.fetchGroups();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(GroupDetail, (props) => `Group ${props.params.groupId}`, refresh, false));
