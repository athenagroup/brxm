/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { mocked } from 'ts-jest/utils';
import { Component, Input } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { Menu, MetaCollection, Page, isMenu } from '@bloomreach/spa-sdk';
import { BrManageMenuButtonDirective } from './br-manage-menu-button.directive';
import { BrPageComponent } from './br-page/br-page.component';

jest.mock('@bloomreach/spa-sdk');

@Component({ template: '<a [brManageMenuButton]="menu"></a>' })
class TestComponent {
  @Input() menu!: Menu;
}

describe('BrManageMenuButtonDirective', () => {
  let menu: Menu;
  let meta: jest.Mocked<MetaCollection>;
  let page: jest.Mocked<Page>;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    menu = { _meta: {} } as unknown as typeof menu;
    meta = {
      clear: jest.fn(),
      render: jest.fn(),
    } as unknown as typeof meta;
    page = {
      getMeta: jest.fn(() => meta),
    } as unknown as typeof page;
  });

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BrManageMenuButtonDirective, TestComponent ],
      providers: [
        { provide: BrPageComponent, useValue: { state: new BehaviorSubject(page) } },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.menu = menu;
    fixture.detectChanges();
  }));

  describe('ngOnChanges', () => {
    it('should create a meta entity', () => {
      expect(page.getMeta).toBeCalledWith(menu._meta);
    });

    it('should use a menu meta entity', () => {
      fixture.componentInstance.menu = { getMeta: jest.fn(() => meta) } as unknown as typeof menu;
      mocked(isMenu).mockReturnValueOnce(true);
      fixture.detectChanges();

      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelector('a'),
        fixture.nativeElement.querySelector('a'),
      );
    });

    it('should render a meta', () => {
      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelector('a'),
        fixture.nativeElement.querySelector('a'),
      );
    });
  });
});
