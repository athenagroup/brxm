/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
"use strict";
(function($) {

    var jQuery = $;
    $.namespace('Hippo.ChannelManager.TemplateComposer.IFrame.UI');

    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Manager = function(options) {
        this.current = null;
        this.containers = {};
        this.dropIndicator = null;
        this.syncRequested = false;
        this.preview = options.previewMode;
        this.resources = options.resources;
        this.init();
    };


    //TODO: looks more like a UI.Page component
    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Manager.prototype = {
        init: function() {
            this.overlay = $('<div/>').addClass('hst-overlay-root').appendTo(document.body);

            try {
                //replace edit link with cms styled button
                var self = this;
                $('.' + HST.CLASS.EDITLINK).each(function(index) {
                    self._createSurfAndEditLink(this);
                });
            } catch(e) {
                sendMessage({msg: 'Error initializing manager.', exception: e}, "iframeexception");
            }
        },

        _createSurfAndEditLink : function(element) {
            var link = Hippo.ChannelManager.TemplateComposer.IFrame.UI.SurfAndEdit.createLink(element);
            var uuid = $(link).attr(HST.ATTR.ID);

            console.log('before add event listeners for '+Hippo.Util.getElementPath(element));
            /**
             * use plain old javascript event listener to prevent other jQuery instances hijacking the event.
             */
            if (link.addEventListener) {
                link.addEventListener('click', function(event) {
                    sendMessage({uuid: uuid}, "edit-document");
                    event.stopPropagation();
                    event.preventDefault();
                    return false;
                }, false);
            } else if (link.attachEvent) {
                link.attachEvent('onclick', function(event) {
                    sendMessage({uuid: uuid}, "edit-document");
                    event.cancelBubble = true;
                    return false;
                });
            }
        },

        createContainers : function(facade) {
            try {
                //attach mouseover/mouseclick for components
                var self = this;
                $('.' + HST.CLASS.CONTAINER).each(function(index) {
                    var container = self._createContainer(this);
                    container.updateSharedData(facade);
                });
            } catch(e) {
                sendMessage({msg: 'Error creating containers.', exception: e}, "iframeexception");
            }
        },

        _createContainer : function(element) {
            var container = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.createOrRetrieve(element);
            this.containers[container.id] = container;
            container.render(this);
            return container;
        },

        _retrieve : function(element) {
            var factory = Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory;
            var id = element.getAttribute(HST.ATTR.ID);
            var o = factory.getById(id);
            if (o == null) {
                Hippo.ChannelManager.TemplateComposer.IFrame.Main.die(this.resources['manager-object-not-found'].format(id));
            }
            return o;
        },

        select: function(element) {
            if (this.current != null && this.current.element == element) {
                return;
            }

            this.current = this._retrieve(element);
            this.current.select();
        },

        deselect : function(element) {
            if (this.current != null) {
                this.current.deselect();
                this.current = null;
            }
        },

        add: function(element, parentId) {
            if (typeof this.containers[parentId] !== 'undefined') {
                var container = this.containers[parentId];
                container.add(element);
                this.checkStateChanges();
            }
        },

        remove : function(element) {
            if (!element.hasAttribute(HST.ATTR.ID)) {
                element = $(element).parents('.'+HST.CLASS.CONTAINER)[0];
            }

            var type = element.getAttribute(HST.ATTR.TYPE);
            var id = element.getAttribute(HST.ATTR.ID);
            var xtype = element.getAttribute(HST.ATTR.XTYPE);

            if (type == HST.CONTAINERITEM) {
                var container = this.containers[id];
                if(!!container && container.removeItem(id)) {
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            } else if (type == HST.CONTAINER) {
                var container = this.containers[id];
                if (!!container) {
                    container.remove();
                    delete this.containers[id];
                    Hippo.ChannelManager.TemplateComposer.IFrame.UI.Factory.deleteObjectRef(id);
                }
            }
            this.checkStateChanges();
        },

        onDragStart : function(ui, container) {
            if(this.dropIndicator == null) {
                this.dropIndicator = $('<div id="hst-drop-indicator"/>').appendTo(document.body);
                this.dropIndicator.css('position', 'absolute');
            }
            this.onDrag(ui, container);
            $.each(this.containers, function(key, value) {
                value.beforeDrag();
            });
        },

        onDrag : function(ui, container) {
            container.drawDropIndicator(ui, this.dropIndicator);
        },

        onDragStop : function() {
            if(this.dropIndicator != null) {
                this.dropIndicator.remove();
                this.dropIndicator = null;
            }
            $.each(this.containers, function(key, value) {
                value.afterDrag();
            });
        },

        //TODO: implement group handling
        highlight : function(groups) {
            $.each(this.containers, function(key, value) {
                value.highlight();
            });
        },

        unhighlight : function(groups) {
            $.each(this.containers, function(key, value) {
                value.unhighlight();
            });
        },

        checkStateChanges : function() {
            $.each(this.containers, function(key, value) {
                value.checkState();
            });
            this.sync();
        },

        requestSync : function() {
            this.syncRequested = true;
        },

        sync : function() {
            if(this.syncRequested) {
                $.each(this.containers, function(key, value) {
                    value.sync();
                });
            }
            this.syncRequested = false;
        },

        updateSharedData : function(facade) {
            $.each(this.containers, function(key, value) {
                value.updateSharedData(facade);
            });
        },

        getOverlay : function() {
            return this.overlay;
        }

    };

})(jQuery);