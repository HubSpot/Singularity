import React, { PropTypes } from 'react';

export default class CollapsableSection extends React.Component {

  static propTypes = {
    defaultExpanded: PropTypes.bool,
    expanded: PropTypes.bool,
    title: PropTypes.string,
    subtitle: PropTypes.string,
    children: PropTypes.node,
    id: PropTypes.string
  };

  constructor(props) {
    super(props);
    this.state = {
      expanded: props.defaultExpanded || props.expanded,
    };
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.expanded !== nextProps.expanded) {
      this.setState({ expanded: nextProps.expanded });
    }
  }

  toggle() {
    this.setState({
      expanded: !this.state.expanded
    });
  }

  render() {
    return (
      <div id={this.props.id}>
        <div className="page-header">
            <h2>
              {this.props.title}
              <small>{this.props.subtitle}</small>
              <small>
                  <a data-action="expandToggle" onClick={() => this.toggle()}>{this.state.expanded ? 'Collapse' : 'View'}</a>
              </small>
            </h2>
        </div>
        {this.state.expanded && this.props.children}
      </div>
    );
  }
}
