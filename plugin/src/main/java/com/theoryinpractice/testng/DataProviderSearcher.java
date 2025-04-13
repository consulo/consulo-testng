/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.theoryinpractice.testng;

import com.intellij.java.indexing.search.searches.MethodReferencesSearch;
import com.intellij.java.indexing.search.searches.MethodReferencesSearchExecutor;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.PsiAnnotation;
import com.intellij.java.language.psi.PsiAnnotationMemberValue;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiNameValuePair;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.UsageSearchContext;
import consulo.project.util.query.QueryExecutorBase;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;

import java.util.function.Predicate;

@ExtensionImpl
public class DataProviderSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>
    implements MethodReferencesSearchExecutor {
    public DataProviderSearcher() {
        super(true);
    }

    @Override
    @RequiredReadAction
    public void processQuery(
        MethodReferencesSearch.SearchParameters queryParameters,
        @Nonnull Predicate<? super PsiReference> consumer
    ) {
        PsiMethod method = queryParameters.getMethod();

        PsiAnnotation annotation = AnnotationUtil.findAnnotation(method, DataProvider.class.getName());
        if (annotation == null) {
            return;
        }
        PsiNameValuePair[] values = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair value : values) {
            if ("name".equals(value.getName())) {
                PsiAnnotationMemberValue dataProviderMethodName = value.getValue();
                if (dataProviderMethodName != null) {
                    String providerName = StringUtil.unquoteString(dataProviderMethodName.getText());
                    queryParameters.getOptimizer()
                        .searchWord(providerName, queryParameters.getScope(), UsageSearchContext.IN_STRINGS, true, method);
                }
            }
        }
    }
}
