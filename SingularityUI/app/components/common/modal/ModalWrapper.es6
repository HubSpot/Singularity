import React from 'react';

export const getClickComponent = (component) => (
  React.Children.map(component.props.children, child => (
    React.cloneElement(child, {
      onClick: () => component.refs.modal.getWrappedInstance().show()
    })
  ))
);
