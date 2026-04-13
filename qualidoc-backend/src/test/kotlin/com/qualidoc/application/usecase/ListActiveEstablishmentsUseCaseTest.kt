package com.qualidoc.application.usecase

import com.qualidoc.TestFixtures
import com.qualidoc.domain.repository.EstablishmentRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListActiveEstablishmentsUseCaseTest {

    private val establishmentRepository = mockk<EstablishmentRepository>()

    private lateinit var useCase: ListActiveEstablishmentsUseCase

    @BeforeEach
    fun setUp() {
        useCase = ListActiveEstablishmentsUseCase(establishmentRepository)
    }

    @Test
    fun `should_return_only_active_establishments`() {
        val active = TestFixtures.anEstablishment(active = true, name = "CHU Toulouse")
        val inactive = TestFixtures.anEstablishment(
            id = TestFixtures.OTHER_ESTABLISHMENT_ID,
            active = false,
            name = "Clinique Fermee",
            code = "CLF"
        )
        every { establishmentRepository.findAll() } returns listOf(active, inactive)

        val result = useCase.execute()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("CHU Toulouse")
        assertThat(result[0].active).isTrue()
    }

    @Test
    fun `should_return_empty_list_when_no_active_establishments`() {
        val inactive = TestFixtures.anEstablishment(active = false)
        every { establishmentRepository.findAll() } returns listOf(inactive)

        val result = useCase.execute()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should_map_establishment_fields_correctly`() {
        val establishment = TestFixtures.anEstablishment()
        every { establishmentRepository.findAll() } returns listOf(establishment)

        val result = useCase.execute()

        assertThat(result[0].id).isEqualTo(establishment.id)
        assertThat(result[0].name).isEqualTo(establishment.name)
        assertThat(result[0].code).isEqualTo(establishment.code)
        assertThat(result[0].active).isEqualTo(establishment.active)
    }
}
