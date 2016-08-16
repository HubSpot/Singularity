import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { FetchGroups } from '../../actions/api/requestGroups';

import { Row, Col, Tabs, Tab } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';
import MetadataButton from '../common/MetadataButton';
import ActionDropdown from './ActionDropdown';

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
  const metadata = !_.isEmpty(group.metadata) && (
    <MetadataButton title={group.id} metadata={group.metadata}>View Metadata</MetadataButton>
  );

  return (
    <div>
      <Row className="detail-header">
        <Col md={7} lg={6}>
          <h1>{group.id}</h1>
        </Col>
        <Col md={5} lg={6} className="button-container">
          <ActionDropdown group={group} />
          {metadata}
        </Col>
      </Row>
      <Tabs id="request-ids">
        {tabs}
      </Tabs>
    </div>
  );
};

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  requests: PropTypes.object
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group
  });
};

const mapDispatchToProps = (dispatch) => {
  return {
    fetchGroups: () => dispatch(FetchGroups.trigger())
  };
};

const refresh = (props) => {
  return props.fetchGroups();
};

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(GroupDetail, (props) => `Group ${props.params.groupId}`, refresh, false));
