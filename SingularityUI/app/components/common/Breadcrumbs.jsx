import React from 'react';
import { Link } from 'react-router';

export default class Breadcrumbs extends React.Component {

  renderItems() {
    return this.props.items.map((item, key) => {
      if (item.link) {
        return (
          <li key={key}>{item.label} <Link to={item.link}>{item.text}</Link></li>
        );
      } else if (item.onClick) {
        return (
          <li key={key}>{item.label} <a onClick={item.onClick}>{item.text}</a></li>
        );
      }
      return (
        <li key={key}>{item.label} {item.text}</li>
      );
    });
  }

  render() {
    return (
      <ul className="breadcrumb clearfix">
          {this.renderItems()}
          <span className="pull-right">{this.props.right}</span>
      </ul>
    );
  }
}

Breadcrumbs.propTypes = {
  items: React.PropTypes.arrayOf(React.PropTypes.shape({
    label: React.PropTypes.string,
    text: React.PropTypes.oneOfType([React.PropTypes.string.isRequired, React.PropTypes.number.isRequired]),
    link: React.PropTypes.string
  })).isRequired,
  right: React.PropTypes.element
};
