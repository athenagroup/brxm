/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { NavItem } from '../../models/dto';
import { NavConfigService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuStructureService } from './menu-structure.service';

@Injectable()
export class MenuBuilderService {
  constructor(
    private navConfigService: NavConfigService,
    private menuStructureService: MenuStructureService,
  ) {}

  buildMenu(): Observable<MenuItem[]> {
    return this.navConfigService.navItems$.pipe(
      map(navItems => {
        const menu = this.menuStructureService.getMenuStructure();

        this.applyNavItems(menu, navItems);
        return this.removeEmptyLeaves(menu);
      }),
      shareReplay(),
    );
  }

  private applyNavItems(menu: MenuItem[], navItems: NavItem[]): void {
    const menuItemLinks = this.getMenuItemLinks(menu);
    navItems.forEach(navItem => {
      let item = menuItemLinks.find(i => i.id === navItem.id);
      if (!item) {
        item = new MenuItemLink(navItem.id, navItem.displayName || navItem.id);
        this.menuStructureService.addExtension(item);
      }
      // One iframe per app is created so app's url can be used as an identifier
      item.appId = navItem.appIframeUrl;
      item.appPath = navItem.appPath;
    });
  }

  private removeEmptyLeaves(menu: MenuItem[]): MenuItem[] {
    return menu.filter(item => {
      if (item instanceof MenuItemLink) {
        return !!item.appPath;
      }

      if (item instanceof MenuItemContainer) {
        item.children = this.removeEmptyLeaves(item.children);
        return Array.isArray(item.children) && item.children.length > 0;
      }

      throw new Error('MenuItem has unknown type.');
    });
  }

  private getMenuItemLinks(menu: MenuItem[]): MenuItemLink[] {
    return menu.reduce(
      (leafs, item) => {
        if (item instanceof MenuItemContainer) {
          return leafs.concat(this.getMenuItemLinks(item.children));
        }
        leafs.push(item);
        return leafs;
      }, []);
  }
}
