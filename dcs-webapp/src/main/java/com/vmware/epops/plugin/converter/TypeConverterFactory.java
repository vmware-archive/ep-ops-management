/*
 * Copyright (c) 2015 VMware, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vmware.epops.plugin.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

/**
 * A factory for "ranged" converters that can convert objects from S to R.
 * 
 * @param <S> The source type converters created by this factory can convert from
 * @param <R> The target type converters created by this factory can convert to;
 */
@Service
public interface TypeConverterFactory<S, R> {
    /**
     * Get the converter to convert from S to target type R
     * 
     * @param <S> the source Type
     * @param targetType the target type to convert to
     * @return A converter from S to R
     */
    Converter<S, R> getConverter(Class<? extends S> sourceType);
}
